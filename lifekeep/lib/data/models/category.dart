import 'package:flutter/material.dart';

class ItemCategory {
  const ItemCategory({
    required this.id,
    required this.name,
    required this.colorValue,
    required this.iconKey,
    this.isDefault = false,
    this.sortOrder = 0,
    required this.createdAt,
  });

  final String id;
  final String name;
  final int colorValue;
  final String iconKey;
  final bool isDefault;
  final int sortOrder;
  final DateTime createdAt;

  /// Fallback category id for orphaned items; cannot be deleted.
  static const otherId = 'cat_other';

  Color get color => Color(colorValue);
  IconData get icon => CategoryIcons.of(iconKey);

  ItemCategory copyWith({String? name, int? colorValue, String? iconKey, int? sortOrder}) => ItemCategory(
        id: id,
        name: name ?? this.name,
        colorValue: colorValue ?? this.colorValue,
        iconKey: iconKey ?? this.iconKey,
        isDefault: isDefault,
        sortOrder: sortOrder ?? this.sortOrder,
        createdAt: createdAt,
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'name': name,
        'color': colorValue,
        'icon': iconKey,
        'is_default': isDefault ? 1 : 0,
        'sort_order': sortOrder,
        'created_at': createdAt.millisecondsSinceEpoch,
      };

  factory ItemCategory.fromMap(Map<String, Object?> m) => ItemCategory(
        id: m['id'] as String,
        name: (m['name'] as String?) ?? '',
        colorValue: (m['color'] as num?)?.toInt() ?? 0xFF64748B,
        iconKey: (m['icon'] as String?) ?? 'folder',
        isDefault: ((m['is_default'] as num?) ?? 0) == 1,
        sortOrder: (m['sort_order'] as num?)?.toInt() ?? 0,
        createdAt: DateTime.fromMillisecondsSinceEpoch((m['created_at'] as num?)?.toInt() ?? 0),
      );
}

/// Fixed icon set (const so icon tree-shaking keeps working).
class CategoryIcons {
  CategoryIcons._();

  static const Map<String, IconData> all = {
    'person': Icons.person_outline_rounded,
    'school': Icons.school_outlined,
    'car': Icons.directions_car_outlined,
    'shield': Icons.shield_outlined,
    'wallet': Icons.account_balance_wallet_outlined,
    'health': Icons.favorite_border_rounded,
    'home': Icons.home_outlined,
    'family': Icons.family_restroom_rounded,
    'folder': Icons.folder_outlined,
    'work': Icons.work_outline_rounded,
    'receipt': Icons.receipt_long_outlined,
    'pet': Icons.pets_outlined,
    'travel': Icons.flight_outlined,
    'star': Icons.star_outline_rounded,
    'book': Icons.menu_book_outlined,
    'build': Icons.build_outlined,
  };

  static IconData of(String key) => all[key] ?? Icons.folder_outlined;
}

/// Default categories created on first launch. Names are stored in the
/// database, so users can rename them.
class DefaultCategories {
  DefaultCategories._();

  static List<ItemCategory> build(bool english) {
    final now = DateTime.now();
    ItemCategory c(String id, String th, String en, int color, String icon, int order) => ItemCategory(
          id: id,
          name: english ? en : th,
          colorValue: color,
          iconKey: icon,
          isDefault: true,
          sortOrder: order,
          createdAt: now,
        );
    return [
      c('cat_personal', 'ส่วนตัว', 'Personal', 0xFF233B67, 'person', 0),
      c('cat_education', 'การศึกษา', 'Education', 0xFF3B6EA8, 'school', 1),
      c('cat_vehicle', 'ยานพาหนะ', 'Vehicle', 0xFF2E7D7A, 'car', 2),
      c('cat_insurance', 'ประกัน', 'Insurance', 0xFF6B5BA8, 'shield', 3),
      c('cat_finance', 'การเงิน', 'Finance', 0xFF2E7D5B, 'wallet', 4),
      c('cat_health', 'สุขภาพ', 'Health', 0xFFB4473F, 'health', 5),
      c('cat_home', 'บ้าน', 'Home', 0xFFC77A2A, 'home', 6),
      c('cat_family', 'ครอบครัว', 'Family', 0xFF9C4F7E, 'family', 7),
      c(ItemCategory.otherId, 'อื่น ๆ', 'Other', 0xFF64748B, 'folder', 8),
    ];
  }
}
