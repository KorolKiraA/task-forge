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
