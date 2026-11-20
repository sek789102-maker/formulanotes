import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/utils/dates.dart';

void main() {
  group('Dates', () {
    test('daysBetween counts calendar days, ignoring time', () {
      expect(Dates.daysBetween(DateTime(2026, 11, 20, 23, 59), DateTime(2026, 11, 21, 0, 1)), 1);
      expect(Dates.daysBetween(DateTime(2026, 11, 20, 8), DateTime(2026, 11, 20, 22)), 0);
      expect(Dates.daysBetween(DateTime(2026, 11, 20), DateTime(2026, 11, 10)), -10);
      expect(Dates.daysBetween(DateTime(2026, 12, 31), DateTime(2027, 1, 1)), 1);
      expect(Dates.daysBetween(DateTime(2028, 2, 28), DateTime(2028, 3, 1)), 2); // leap year
    });

    test('addMonths clamps the day', () {
      expect(Dates.addMonths(DateTime(2026, 1, 31), 1), DateTime(2026, 2, 28));
      expect(Dates.addMonths(DateTime(2026, 12, 15), 1), DateTime(2027, 1, 15));
      expect(Dates.addMonths(DateTime(2026, 1, 15), -1), DateTime(2025, 12, 15));
    });

    test('monthGrid has 42 days starting on Sunday and contains the whole month', () {
      final g = Dates.monthGrid(DateTime(2026, 11));
      expect(g.length, 42);
      expect(g.first.weekday, DateTime.sunday);
      expect(g.first, DateTime(2026, 11, 1)); // 1 Nov 2026 is a Sunday
      expect(g.where((d) => d.month == 11).length, 30);
      final feb = Dates.monthGrid(DateTime(2027, 2));
      expect(feb.first, DateTime(2027, 1, 31));
    });
  });

  group('ExpiryCalc', () {
    final now = DateTime(2026, 11, 20, 10);
    test('status thresholds', () {
      expect(ExpiryCalc.status(null, now), ExpiryStatus.none);
      expect(ExpiryCalc.status(DateTime(2026, 11, 19), now), ExpiryStatus.expired);
      expect(ExpiryCalc.status(DateTime(2026, 11, 20), now), ExpiryStatus.expiresToday);
      expect(ExpiryCalc.status(DateTime(2026, 11, 21), now), ExpiryStatus.expiringSoon);
      expect(ExpiryCalc.status(DateTime(2026, 12, 20), now), ExpiryStatus.expiringSoon); // 30 days
      expect(ExpiryCalc.status(DateTime(2026, 12, 21), now), ExpiryStatus.valid); // 31 days
    });
    test('daysLeft', () {
      expect(ExpiryCalc.daysLeft(DateTime(2026, 11, 27), now), 7);
      expect(ExpiryCalc.daysLeft(DateTime(2026, 11, 18), now), -2);
    });
  });

  group('ReminderCalc', () {
    test('document anchor uses reminder time of day', () {
      expect(ReminderCalc.documentAnchor(DateTime(2027, 1, 10), 9, 30), DateTime(2027, 1, 10, 9, 30));
    });

    test('fire times subtract offsets, drop past, dedupe and sort', () {
      final anchor = DateTime(2026, 12, 10, 9);
      final now = DateTime(2026, 12, 5, 12);
      final r = ReminderCalc.fireTimes(anchor, [
        ReminderCalc.sevenDays,
        ReminderCalc.oneDay,
        ReminderCalc.threeDays,
        ReminderCalc.oneDay,
        0,
      ], now);
      expect(r, [DateTime(2026, 12, 7, 9), DateTime(2026, 12, 9, 9), DateTime(2026, 12, 10, 9)]);
    });

    test('custom minute/hour offsets', () {
      final anchor = DateTime(2026, 12, 10, 9);
      final r = ReminderCalc.fireTimes(anchor, [30, 120, 1440 + 60], DateTime(2026, 1, 1));
      expect(r, [DateTime(2026, 12, 9, 8), DateTime(2026, 12, 10, 7), DateTime(2026, 12, 10, 8, 30)]);
    });

    test('invalid offsets are ignored', () {
      final r = ReminderCalc.fireTimes(DateTime(2026, 12, 10), [-5, ReminderCalc.maxOffset + 1], DateTime(2020));
      expect(r, isEmpty);
    });

    test('item anchor for date-only items uses default time', () {
      expect(ReminderCalc.itemAnchor(DateTime(2026, 5, 1), hasTime: false, defaultHour: 8), DateTime(2026, 5, 1, 8));
      expect(ReminderCalc.itemAnchor(DateTime(2026, 5, 1, 14, 5), hasTime: true), DateTime(2026, 5, 1, 14, 5));
    });
  });

  group('TaskGrouping', () {
    final now = DateTime(2026, 11, 20, 12);
    TaskGroup g(DateTime? due, {bool time = false, bool done = false}) =>
        TaskGrouping.classify(completed: done, dueAt: due, hasTime: time, now: now);

    test('groups', () {
      expect(g(DateTime(2026, 11, 20), done: true), TaskGroup.completed);
      expect(g(null), TaskGroup.upcoming);
      expect(g(DateTime(2026, 11, 19)), TaskGroup.overdue);
      expect(g(DateTime(2026, 11, 20)), TaskGroup.today);
      expect(g(DateTime(2026, 11, 20, 18), time: true), TaskGroup.today);
      expect(g(DateTime(2026, 11, 20, 9), time: true), TaskGroup.overdue);
      expect(g(DateTime(2026, 11, 21)), TaskGroup.upcoming);
    });
  });
}
