import 'package:flutter/foundation.dart';

import '../l10n/lang.dart';

enum ErrorKind {
  database,
  file,
  permission,
  notification,
  backup,
  restore,
  validation,
  security,
  unknown,
}

/// An error with a message that is safe to show to the user.
/// Technical details are kept in [cause] and only logged in debug mode.
class AppException implements Exception {
  AppException(this.kind, this.userMessage, [this.cause]);

  final ErrorKind kind;
  final String userMessage;
  final Object? cause;

  @override
  String toString() => 'AppException($kind): $userMessage${cause != null ? ' / $cause' : ''}';
}

/// Converts any thrown object into a friendly, localized message.
String friendlyError(Object error) {
  if (error is AppException) return error.userMessage;
  if (kDebugMode) debugPrint('Unexpected error: $error');
  return Lang.t(
    'เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง',
    'Something went wrong. Please try again.',
  );
}

AppException dbError(Object cause) => AppException(
      ErrorKind.database,
      Lang.t('ไม่สามารถบันทึกหรืออ่านข้อมูลได้ กรุณาลองใหม่อีกครั้ง',
          'Could not read or save data. Please try again.'),
      cause,
    );

AppException fileError(Object cause) => AppException(
      ErrorKind.file,
      Lang.t('ไม่สามารถจัดการไฟล์ได้ ไฟล์อาจถูกย้าย ลบ หรือพื้นที่เก็บข้อมูลไม่พอ',
          'Could not handle the file. It may have been moved, deleted, or storage is full.'),
      cause,
    );
