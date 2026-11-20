import 'dart:convert';

import '../../core/utils/dates.dart';

/// Reminder settings shared by documents, tasks and events: a set of
/// offsets (in minutes) before the item's anchor time.
class ReminderConfig {
  const ReminderConfig(this.offsetsMinutes);

  const ReminderConfig.none() : offsetsMinutes = const [];

  final List<int> offsetsMinutes;

  bool get isEnabled => offsetsMinutes.isNotEmpty;

  static const presetsDocument = [ReminderCalc.oneDay, ReminderCalc.threeDays, ReminderCalc.sevenDays];
  static const presetsItem = [
    ReminderCalc.atTime,
    ReminderCalc.oneDay,
    ReminderCalc.threeDays,
    ReminderCalc.sevenDays,
  ];

  List<int> get customOffsets =>
      offsetsMinutes.where((o) => !presetsItem.contains(o)).toList();

  ReminderConfig normalized() {
    final s = offsetsMinutes.where((o) => o >= 0 && o <= ReminderCalc.maxOffset).toSet().toList()..sort();
    return ReminderConfig(s);
  }

  ReminderConfig toggle(int offset) {
    final s = offsetsMinutes.toSet();
    if (!s.remove(offset)) s.add(offset);
    return ReminderConfig(s.toList()).normalized();
  }

  String encode() => jsonEncode({'offsets': normalized().offsetsMinutes});

  static ReminderConfig decode(String? raw) {
    if (raw == null || raw.isEmpty) return const ReminderConfig.none();
    try {
      final m = jsonDecode(raw);
      if (m is Map && m['offsets'] is List) {
        return ReminderConfig((m['offsets'] as List).whereType<num>().map((e) => e.toInt()).toList())
            .normalized();
      }
    } catch (_) {}
    return const ReminderConfig.none();
  }

  @override
  bool operator ==(Object other) =>
      other is ReminderConfig && other.normalized().encode() == normalized().encode();

  @override
  int get hashCode => normalized().encode().hashCode;
}
