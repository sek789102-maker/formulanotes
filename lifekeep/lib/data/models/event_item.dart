import 'reminder_config.dart';

class EventItem {
  const EventItem({
    required this.id,
    required this.title,
    this.description = '',
    this.location = '',
    required this.startAt,
    this.endAt,
    this.allDay = false,
    this.reminder = const ReminderConfig.none(),
    this.colorValue,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final String description;
  final String location;
  final DateTime startAt;
  final DateTime? endAt;
  final bool allDay;
  final ReminderConfig reminder;
  final int? colorValue;
  final DateTime createdAt;
  final DateTime updatedAt;

  /// Whether the event occurs on calendar day [day] (multi-day aware).
  bool occursOn(DateTime day) {
    final d0 = DateTime(day.year, day.month, day.day);
    final s = DateTime(startAt.year, startAt.month, startAt.day);
    final e0 = endAt ?? startAt;
    final e = DateTime(e0.year, e0.month, e0.day);
    return !d0.isBefore(s) && !d0.isAfter(e);
  }

  EventItem copyWith({
    String? title,
    String? description,
    String? location,
    DateTime? startAt,
    DateTime? endAt,
    bool clearEnd = false,
    bool? allDay,
    ReminderConfig? reminder,
    int? colorValue,
    DateTime? updatedAt,
  }) =>
      EventItem(
        id: id,
        title: title ?? this.title,
        description: description ?? this.description,
        location: location ?? this.location,
        startAt: startAt ?? this.startAt,
        endAt: clearEnd ? null : (endAt ?? this.endAt),
        allDay: allDay ?? this.allDay,
        reminder: reminder ?? this.reminder,
        colorValue: colorValue ?? this.colorValue,
        createdAt: createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'title': title,
        'description': description,
        'location': location,
        'start_at': startAt.millisecondsSinceEpoch,
        'end_at': endAt?.millisecondsSinceEpoch,
        'all_day': allDay ? 1 : 0,
        'reminder': reminder.encode(),
        'color': colorValue,
        'created_at': createdAt.millisecondsSinceEpoch,
        'updated_at': updatedAt.millisecondsSinceEpoch,
      };

  factory EventItem.fromMap(Map<String, Object?> m) {
    final end = (m['end_at'] as num?)?.toInt();
    return EventItem(
      id: m['id'] as String,
      title: (m['title'] as String?) ?? '',
      description: (m['description'] as String?) ?? '',
      location: (m['location'] as String?) ?? '',
      startAt: DateTime.fromMillisecondsSinceEpoch((m['start_at'] as num?)?.toInt() ?? 0),
      endAt: end == null ? null : DateTime.fromMillisecondsSinceEpoch(end),
      allDay: ((m['all_day'] as num?) ?? 0) == 1,
      reminder: ReminderConfig.decode(m['reminder'] as String?),
      colorValue: (m['color'] as num?)?.toInt(),
      createdAt: DateTime.fromMillisecondsSinceEpoch((m['created_at'] as num?)?.toInt() ?? 0),
      updatedAt: DateTime.fromMillisecondsSinceEpoch((m['updated_at'] as num?)?.toInt() ?? 0),
    );
  }
}
