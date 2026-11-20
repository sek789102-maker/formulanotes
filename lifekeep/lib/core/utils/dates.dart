/// Pure date helpers (no Flutter dependency) so they can be unit-tested.
class Dates {
  Dates._();

  static DateTime startOfDay(DateTime d) => DateTime(d.year, d.month, d.day);

  static DateTime endOfDay(DateTime d) => DateTime(d.year, d.month, d.day, 23, 59, 59, 999);

  static bool isSameDay(DateTime a, DateTime b) => a.year == b.year && a.month == b.month && a.day == b.day;

  /// Calendar-day difference (b - a), DST safe (uses UTC dates).
  static int daysBetween(DateTime a, DateTime b) {
    final ua = DateTime.utc(a.year, a.month, a.day);
    final ub = DateTime.utc(b.year, b.month, b.day);
    return ub.difference(ua).inDays;
  }

  static DateTime firstDayOfMonth(DateTime d) => DateTime(d.year, d.month);

  static int daysInMonth(int year, int month) => DateTime(year, month + 1, 0).day;

  static DateTime addMonths(DateTime d, int months) {
    final total = d.year * 12 + (d.month - 1) + months;
    final y = total ~/ 12;
    final m = total % 12 + 1;
    final day = d.day.clamp(1, daysInMonth(y, m));
    return DateTime(y, m, day, d.hour, d.minute);
  }

  /// The 42 days (6 weeks) shown in a month grid. Weeks start on Sunday
  /// (common in Thailand) unless [mondayFirst] is set.
  static List<DateTime> monthGrid(DateTime month, {bool mondayFirst = false}) {
    final first = firstDayOfMonth(month);
    final weekday = first.weekday % 7; // Sunday=0
    final offset = mondayFirst ? (first.weekday - 1) : weekday;
    final start = DateTime(first.year, first.month, 1 - offset);
    return List.generate(42, (i) => DateTime(start.year, start.month, start.day + i));
  }
}

enum ExpiryStatus { none, valid, expiringSoon, expiresToday, expired }

class ExpiryCalc {
  ExpiryCalc._();

  /// Documents expiring within this many days count as "expiring soon".
  static const soonThresholdDays = 30;

  /// Days from [now] (calendar days) until [expiry]. Negative = expired.
  static int? daysLeft(DateTime? expiry, DateTime now) => expiry == null ? null : Dates.daysBetween(now, expiry);

  static ExpiryStatus status(DateTime? expiry, DateTime now, {int soonDays = soonThresholdDays}) {
    final d = daysLeft(expiry, now);
    if (d == null) return ExpiryStatus.none;
    if (d < 0) return ExpiryStatus.expired;
    if (d == 0) return ExpiryStatus.expiresToday;
    if (d <= soonDays) return ExpiryStatus.expiringSoon;
    return ExpiryStatus.valid;
  }
}

class ReminderCalc {
  ReminderCalc._();

  static const minute = 1;
  static const hour = 60;
  static const day = 1440;

  static const atTime = 0;
  static const oneDay = day;
  static const threeDays = 3 * day;
  static const sevenDays = 7 * day;
  static const maxOffset = 366 * day;

  /// Point in time a document reminder is anchored to: the expiry date at
  /// the user's preferred reminder time of day.
  static DateTime documentAnchor(DateTime expiry, int hour, int minute) =>
      DateTime(expiry.year, expiry.month, expiry.day, hour, minute);

  /// Anchor for a task/event. Date-only items use the default time of day.
  static DateTime itemAnchor(DateTime at, {required bool hasTime, int defaultHour = 9, int defaultMinute = 0}) =>
      hasTime ? at : DateTime(at.year, at.month, at.day, defaultHour, defaultMinute);

  /// Computes the notification fire times for [anchor] and [offsetsMinutes].
  /// Past times are dropped, duplicates removed, result sorted ascending.
  static List<DateTime> fireTimes(DateTime anchor, Iterable<int> offsetsMinutes, DateTime now) {
    final set = <int>{};
    for (final o in offsetsMinutes) {
      if (o < 0 || o > maxOffset) continue;
      // Subtract calendar days/hours via DateTime fields so DST shifts keep
      // the same wall-clock time for day-based offsets.
      final days = o ~/ day;
      final rest = o % day;
      final t = DateTime(
        anchor.year,
        anchor.month,
        anchor.day - days,
        anchor.hour,
        anchor.minute,
      ).subtract(Duration(minutes: rest));
      if (t.isAfter(now)) {
        set.add(DateTime(t.year, t.month, t.day, t.hour, t.minute).millisecondsSinceEpoch);
      }
    }
    final list = set.toList()..sort();
    return list.map((ms) => DateTime.fromMillisecondsSinceEpoch(ms)).toList();
  }
}

enum TaskGroup { today, upcoming, overdue, completed }

class TaskGrouping {
  TaskGrouping._();

  /// Classifies a task. Tasks without a due date are "upcoming".
  /// A timed task due earlier today is overdue; a date-only task due today
  /// stays in "today" until the day ends.
  static TaskGroup classify({
    required bool completed,
    required DateTime? dueAt,
    required bool hasTime,
    required DateTime now,
  }) {
    if (completed) return TaskGroup.completed;
    if (dueAt == null) return TaskGroup.upcoming;
    final dayDiff = Dates.daysBetween(now, dueAt);
    if (dayDiff < 0) return TaskGroup.overdue;
    if (dayDiff == 0) {
      if (hasTime && dueAt.isBefore(now)) return TaskGroup.overdue;
      return TaskGroup.today;
    }
    return TaskGroup.upcoming;
  }
}
