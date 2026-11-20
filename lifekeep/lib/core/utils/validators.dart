import '../l10n/lang.dart';

class Validators {
  Validators._();

  static const maxTitle = 120;
  static const maxText = 5000;

  /// Returns an error message or null when valid.
  static String? title(String? v) {
    final s = v?.trim() ?? '';
    if (s.isEmpty) return Lang.t('กรุณากรอกชื่อ', 'Please enter a title');
    if (s.length > maxTitle) {
      return Lang.t('ชื่อยาวเกิน $maxTitle ตัวอักษร', 'Title is longer than $maxTitle characters');
    }
    return null;
  }

  static String? longText(String? v) {
    if ((v ?? '').length > maxText) {
      return Lang.t('ข้อความยาวเกิน $maxText ตัวอักษร', 'Text is longer than $maxText characters');
    }
    return null;
  }

  static String? categoryName(String? v) {
    final s = v?.trim() ?? '';
    if (s.isEmpty) return Lang.t('กรุณากรอกชื่อหมวดหมู่', 'Please enter a category name');
    if (s.length > 40) return Lang.t('ชื่อหมวดหมู่ยาวเกินไป', 'ItemCategory name is too long');
    return null;
  }

  /// End must not be before start.
  static String? eventRange(DateTime start, DateTime? end) {
    if (end != null && end.isBefore(start)) {
      return Lang.t('เวลาสิ้นสุดต้องไม่ก่อนเวลาเริ่ม', 'End time must not be before start time');
    }
    return null;
  }

  /// Custom reminder amount must be a positive integer within reason.
  static String? customReminder(String? v, {required int maxMinutes, required int unitMinutes}) {
    final n = int.tryParse((v ?? '').trim());
    if (n == null || n <= 0) return Lang.t('กรุณากรอกตัวเลขมากกว่า 0', 'Enter a number greater than 0');
    if (n * unitMinutes > maxMinutes) {
      return Lang.t('ระยะเวลาเกิน 1 ปี', 'Longer than 1 year');
    }
    return null;
  }

  static bool isValidPin(String pin) => RegExp(r'^\d{4,6}$').hasMatch(pin);
}
