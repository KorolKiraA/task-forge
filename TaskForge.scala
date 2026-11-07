import scala.collection.mutable
import scala.io.StdIn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * TaskForge — приоритетная очередь задач службы поддержки (helpdesk-core).
 *
 * Коммерческая ценность: ядро helpdesk/ticket-системы — заявки с приоритетами
 * (SLA), эскалация просроченных задач, назначение на инженеров, метрики
 * (среднее время реакции, backlog). Приоритетная очередь гарантирует, что
 * критичные инциденты обрабатываются первыми — основа SLA-менеджмента.
 *
 * Запуск: scala-cli run TaskForge.scala
 */
object TaskForge:

  enum Priority(val weight: Int, val slaHours: Int):
    case Critical extends Priority(1, 4)
    case High     extends Priority(2, 24)
    case Medium   extends Priority(3, 72)
    case Low      extends Priority(4, 168)

  case class Ticket(id: Int, title: String, priority: Priority,
                    created: LocalDateTime, assignee: Option[String] = None,
                    done: Boolean = false):
    def deadline: LocalDateTime = created.plusHours(priority.slaHours)
    def overdue: Boolean = !done && LocalDateTime.now.isAfter(deadline)

  // Очередь: сначала по весу приоритета, затем FIFO по id
  given Ordering[Ticket] = Ordering.by(t => (t.priority.weight, t.id)).reverse

  val queue = mutable.PriorityQueue.empty[Ticket]
  val resolved = mutable.ListBuffer.empty[Ticket]
  var nextId = 1000
  val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

  def main(args: Array[String]): Unit =
    seed()
    println("=== TaskForge v1.0 — приоритетная очередь заявок helpdesk ===")
    var running = true
    while running do
      escalateCheck()
      print("""
          |
          |1) Новая заявка     2) Взять следующую (по приоритету)
          |3) Назначить        4) Завершить          5) Очередь
          |6) Метрики SLA      0) Выход
          |> """.stripMargin)
      StdIn.readLine().trim match
        case "1" => createTicket()
        case "2" => takeNext()
        case "3" => assign()
        case "4" => complete()
        case "5" => showQueue()
        case "6" => metrics()
        case "0" => running = false
        case _   => println("Неизвестная команда.")
    println("Смена завершена. Хорошего дня!")

  def seed(): Unit =
    enqueue("Падает платёжный шлюз", Priority.Critical)
    enqueue("Не приходят письма клиентам", Priority.High)
    enqueue("Обновить логотип на сайте", Priority.Low)
    enqueue("Медленный отчёт в CRM", Priority.Medium)

  def enqueue(title: String, p: Priority): Ticket =
    nextId += 1
    val t = Ticket(nextId, title, p, LocalDateTime.now)
    queue.enqueue(t)
    t

  def createTicket(): Unit =
    print("Описание проблемы: ")
    val title = StdIn.readLine().trim
    print("Приоритет (1=Critical 2=High 3=Medium 4=Low): ")
    val p = Priority.fromOrdinal(StdIn.readLine().trim.toInt - 1)
    val t = enqueue(title, p)
    println(s"Заявка #${t.id} создана. SLA: ${p.slaHours} ч (до ${t.deadline.format(fmt)})")

  def takeNext(): Unit =
    if queue.isEmpty then println("Очередь пуста — можно пить кофе ☕")
    else
      val t = queue.dequeue()
      queue.enqueue(t) // заглянули, но не забрали
      println(s"Следующая по приоритету: #${t.id} [${t.priority}] ${t.title}")
      println(s"  Дедлайн SLA: ${t.deadline.format(fmt)}${if t.overdue then " — ПРОСРОЧЕНА!" else ""}")

  def assign(): Unit =
    print("ID заявки: "); val id = StdIn.readLine().trim.toInt
    print("Инженер: "); val who = StdIn.readLine().trim
    val rest = queue.clone().dequeueAll
    val (hit, miss) = rest.partition(_.id == id)
    hit.headOption match
      case Some(t) => queue.enqueue(miss :+ t.copy(assignee = Some(who)): _*)
                      println(s"#$id назначена на $who")
      case None    => queue.enqueue(miss: _*); println("Заявка не найдена.")

  def complete(): Unit =
    print("ID заявки: "); val id = StdIn.readLine().trim.toInt
    val all = queue.clone().dequeueAll
    val (hit, miss) = all.partition(_.id == id)
    hit.headOption match
      case Some(t) =>
        resolved += t.copy(done = true)
        queue.enqueue(miss: _*)
        val slaOk = !LocalDateTime.now.isAfter(t.deadline)
        println(s"#$id закрыта. SLA ${if slaOk then "выполнен ✔" else "НАРУШЕН ✘"}")
      case None => queue.enqueue(miss: _*); println("Заявка не найдена.")

  def showQueue(): Unit =
    if queue.isEmpty then println("Очередь пуста.")
    else
      println(f"${"ID"}%-6s ${"Приоритет"}%-10s ${"SLA до"}%-18s ${"Инженер"}%-12s Заявка")
      queue.clone().dequeueAll.foreach { t =>
        val flag = if t.overdue then " ⚠" else ""
        println(f"${t.id}%-6d ${t.priority.toString}%-10s ${t.deadline.format(fmt)}%-18s ${t.assignee.getOrElse("—")}%-12s ${t.title}$flag")
      }

  def metrics(): Unit =
    val overdueCount = queue.count(_.overdue)
    println(f"""
        |--- Метрики SLA ---
        |Открыто заявок:      ${queue.size}
        |  Critical/High:     ${queue.count(_.priority.weight <= 2)}
        |Просрочено по SLA:   $overdueCount
        |Закрыто за сессию:   ${resolved.size}
        |-------------------""".stripMargin)

  def escalateCheck(): Unit =
    queue.count(_.overdue) match
      case 0 => ()
      case n => println(s"⚠ ЭСКАЛАЦИЯ: $n заявок просрочены по SLA! Уведомить руководителя.")
