import 'dart:convert';

import 'reminder_config.dart';

class DocumentItem {
  const DocumentItem({
    required this.id,
    required this.title,
    required this.categoryId,
    this.description = '',
    this.notes = '',
    required this.createdAt,
    required this.updatedAt,
    this.expiryDate,
    this.reminder = const ReminderConfig.none(),
    this.attachmentPaths = const [],
    this.isPinned = false,
  });

  final String id;
  final String title;
  final String categoryId;
  final String description;
  final String notes;
  final DateTime createdAt;
  final DateTime updatedAt;
  final DateTime? expiryDate;
  final ReminderConfig reminder;

  /// File names relative to the app's attachments directory.
  final List<String> attachmentPaths;
  final bool isPinned;

  DocumentItem copyWith({
    String? title,
    String? categoryId,
    String? description,
    String? notes,
    DateTime? updatedAt,
    DateTime? expiryDate,
    bool clearExpiry = false,
    ReminderConfig? reminder,
    List<String>? attachmentPaths,
    bool? isPinned,
  }) => DocumentItem(
    id: id,
    title: title ?? this.title,
    categoryId: categoryId ?? this.categoryId,
    description: description ?? this.description,
    notes: notes ?? this.notes,
    createdAt: createdAt,
    updatedAt: updatedAt ?? this.updatedAt,
    expiryDate: clearExpiry ? null : (expiryDate ?? this.expiryDate),
    reminder: reminder ?? this.reminder,
    attachmentPaths: attachmentPaths ?? this.attachmentPaths,
    isPinned: isPinned ?? this.isPinned,
  );

  Map<String, Object?> toMap() => {
    'id': id,
    'title': title,
    'category_id': categoryId,
    'description': description,
    'notes': notes,
    'created_at': createdAt.millisecondsSinceEpoch,
    'updated_at': updatedAt.millisecondsSinceEpoch,
    'expiry_date': expiryDate?.millisecondsSinceEpoch,
    'reminder': reminder.encode(),
    'attachments': jsonEncode(attachmentPaths),
    'is_pinned': isPinned ? 1 : 0,
  };

  factory DocumentItem.fromMap(Map<String, Object?> m) {
    List<String> atts = const [];
    try {
      final raw = m['attachments'];
      if (raw is String && raw.isNotEmpty) {
        atts = (jsonDecode(raw) as List).whereType<String>().toList();
      }
    } catch (_) {}
    final exp = (m['expiry_date'] as num?)?.toInt();
    return DocumentItem(
      id: m['id'] as String,
      title: (m['title'] as String?) ?? '',
      categoryId: (m['category_id'] as String?) ?? 'cat_other',
      description: (m['description'] as String?) ?? '',
      notes: (m['notes'] as String?) ?? '',
      createdAt: DateTime.fromMillisecondsSinceEpoch((m['created_at'] as num?)?.toInt() ?? 0),
      updatedAt: DateTime.fromMillisecondsSinceEpoch((m['updated_at'] as num?)?.toInt() ?? 0),
      expiryDate: exp == null ? null : DateTime.fromMillisecondsSinceEpoch(exp),
      reminder: ReminderConfig.decode(m['reminder'] as String?),
      attachmentPaths: atts,
      isPinned: ((m['is_pinned'] as num?) ?? 0) == 1,
    );
  }
}
