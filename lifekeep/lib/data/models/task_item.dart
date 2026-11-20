import 'reminder_config.dart';

enum TaskPriority { low, normal, high }

class TaskItem {
  const TaskItem({
    required this.id,
    required this.title,
    this.description = '',
    this.categoryId,
    this.dueAt,
    this.hasTime = false,
    this.priority = TaskPriority.normal,
    this.isCompleted = false,
    this.completedAt,
    this.reminder = const ReminderConfig.none(),
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final String description;
  final String? categoryId;
  final DateTime? dueAt;
  final bool hasTime;
  final TaskPriority priority;
  final bool isCompleted;
  final DateTime? completedAt;
  final ReminderConfig reminder;
  final DateTime createdAt;
  final DateTime updatedAt;

  TaskItem copyWith({
    String? title,
    String? description,
    String? categoryId,
    bool clearCategory = false,
    DateTime? dueAt,
    bool clearDue = false,
    bool? hasTime,
    TaskPriority? priority,
    bool? isCompleted,
    DateTime? completedAt,
    bool clearCompletedAt = false,
    ReminderConfig? reminder,
    DateTime? updatedAt,
  }) => TaskItem(
    id: id,
    title: title ?? this.title,
    description: description ?? this.description,
    categoryId: clearCategory ? null : (categoryId ?? this.categoryId),
    dueAt: clearDue ? null : (dueAt ?? this.dueAt),
    hasTime: hasTime ?? this.hasTime,
    priority: priority ?? this.priority,
    isCompleted: isCompleted ?? this.isCompleted,
    completedAt: clearCompletedAt ? null : (completedAt ?? this.completedAt),
    reminder: reminder ?? this.reminder,
    createdAt: createdAt,
    updatedAt: updatedAt ?? this.updatedAt,
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'title': title,
    'description': description,
    'category_id': categoryId,
    'due_at': dueAt?.millisecondsSinceEpoch,
    'has_time': hasTime ? 1 : 0,
    'priority': priority.index,
    'is_completed': isCompleted ? 1 : 0,
    'completed_at': completedAt?.millisecondsSinceEpoch,
    'reminder': reminder.encode(),
    'created_at': createdAt.millisecondsSinceEpoch,
    'updated_at': updatedAt.millisecondsSinceEpoch,
  };

  factory TaskItem.fromMap(Map<String, Object?> m) {
    final due = (m['due_at'] as num?)?.toInt();
    final comp = (m['completed_at'] as num?)?.toInt();
    final p = (m['priority'] as num?)?.toInt() ?? 1;
    return TaskItem(
      id: m['id'] as String,
      title: (m['title'] as String?) ?? '',
      description: (m['description'] as String?) ?? '',
      categoryId: m['category_id'] as String?,
      dueAt: due == null ? null : DateTime.fromMillisecondsSinceEpoch(due),
      hasTime: ((m['has_time'] as num?) ?? 0) == 1,
      priority: TaskPriority.values[p.clamp(0, TaskPriority.values.length - 1)],
      isCompleted: ((m['is_completed'] as num?) ?? 0) == 1,
      completedAt: comp == null ? null : DateTime.fromMillisecondsSinceEpoch(comp),
      reminder: ReminderConfig.decode(m['reminder'] as String?),
      createdAt: DateTime.fromMillisecondsSinceEpoch((m['created_at'] as num?)?.toInt() ?? 0),
      updatedAt: DateTime.fromMillisecondsSinceEpoch((m['updated_at'] as num?)?.toInt() ?? 0),
    );
  }
}
