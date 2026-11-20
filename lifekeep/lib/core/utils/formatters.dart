import 'package:intl/intl.dart';

import '../l10n/lang.dart';

/// Locale-aware date/time formatting. Thai uses Buddhist Era (พ.ศ.) years.
class Fmt {
  Fmt._();

  static const _thMonths = [
    'มกราคม',
    'กุมภาพันธ์',
    'มีนาคม',
    'เมษายน',
    'พฤษภาคม',
    'มิถุนายน',
    'กรกฎาคม',
    'สิงหาคม',
    'กันยายน',
    'ตุลาคม',
    'พฤศจิกายน',
    'ธันวาคม',
  ];
  static const _thMonthsShort = [
    'ม.ค.',
    'ก.พ.',
    'มี.ค.',
    'เม.ย.',
    'พ.ค.',
    'มิ.ย.',
    'ก.ค.',
    'ส.ค.',
    'ก.ย.',
    'ต.ค.',
    'พ.ย.',
    'ธ.ค.',
  ];
  static const _thWeekdays = ['จันทร์', 'อังคาร', 'พุธ', 'พฤหัสบดี', 'ศุกร์', 'เสาร์', 'อาทิตย์'];
  static const _thWeekdaysShort = ['จ', 'อ', 'พ', 'พฤ', 'ศ', 'ส', 'อา'];
  static const _enWeekdaysShort = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  static int _beYear(int y) => y + 543;

  /// e.g. "20 พ.ย. 2569" / "20 Nov 2026"
  static String date(DateTime d) {
    if (Lang.isEn) return DateFormat('d MMM y', 'en_US').format(d);
    return '${d.day} ${_thMonthsShort[d.month - 1]} ${_beYear(d.year)}';
  }

  /// e.g. "วันศุกร์ที่ 20 พฤศจิกายน 2569"
  static String fullDate(DateTime d) {
    if (Lang.isEn) return DateFormat('EEEE, d MMMM y', 'en_US').format(d);
    return 'วัน${_thWeekdays[d.weekday - 1]}ที่ ${d.day} ${_thMonths[d.month - 1]} ${_beYear(d.year)}';
  }

  static String monthYear(DateTime d) {
    if (Lang.isEn) return DateFormat('MMMM y', 'en_US').format(d);
    return '${_thMonths[d.month - 1]} ${_beYear(d.year)}';
  }

  static String time(DateTime d) => '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  static String dateTime(DateTime d) => '${date(d)} · ${time(d)}';

  static String weekdayShort(int weekday) => Lang.isEn ? _enWeekdaysShort[weekday - 1] : _thWeekdaysShort[weekday - 1];

  /// "อีก 3 วัน", "วันนี้", "พรุ่งนี้", "เลยมา 2 วัน"
  static String relativeDays(int days) {
    if (days == 0) return Lang.t('วันนี้', 'Today');
    if (days == 1) return Lang.t('พรุ่งนี้', 'Tomorrow');
    if (days == -1) return Lang.t('เมื่อวาน', 'Yesterday');
    if (days > 0) return Lang.t('อีก $days วัน', 'In $days days');
    return Lang.t('เลยมา ${-days} วัน', '${-days} days ago');
  }

  static String fileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
  }

  /// Describes a reminder offset in minutes, e.g. "1 วันก่อน".
  static String reminderOffset(int minutes) {
    if (minutes == 0) return Lang.t('ตรงเวลา', 'At time');
    if (minutes % 1440 == 0) {
      final d = minutes ~/ 1440;
      return Lang.t('$d วันก่อน', d == 1 ? '1 day before' : '$d days before');
    }
    if (minutes % 60 == 0) {
      final h = minutes ~/ 60;
      return Lang.t('$h ชั่วโมงก่อน', h == 1 ? '1 hour before' : '$h hours before');
    }
    return Lang.t('$minutes นาทีก่อน', '$minutes min before');
  }
}
