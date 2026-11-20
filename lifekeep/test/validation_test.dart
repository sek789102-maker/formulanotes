import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/utils/validators.dart';
import 'package:lifekeep/data/models/reminder_config.dart';
import 'package:lifekeep/data/models/task_item.dart';
import 'package:lifekeep/data/repositories/sql_utils.dart';
import 'package:lifekeep/services/attachment_service.dart';

void main() {
  test('title validation', () {
    expect(Validators.title(''), isNotNull);
    expect(Validators.title('   '), isNotNull);
    expect(Validators.title('x' * 121), isNotNull);
    expect(Validators.title('ใบขับขี่'), isNull);
  });

  test('event range', () {
    final s = DateTime(2026, 1, 2);
    expect(Validators.eventRange(s, DateTime(2026, 1, 1)), isNotNull);
    expect(Validators.eventRange(s, s), isNull);
    expect(Validators.eventRange(s, null), isNull);
  });

  test('custom reminder input', () {
    expect(Validators.customReminder('0', maxMinutes: 527040, unitMinutes: 1440), isNotNull);
    expect(Validators.customReminder('abc', maxMinutes: 527040, unitMinutes: 1440), isNotNull);
    expect(Validators.customReminder('400', maxMinutes: 527040, unitMinutes: 1440), isNotNull);
    expect(Validators.customReminder('14', maxMinutes: 527040, unitMinutes: 1440), isNull);
  });

  test('PIN format', () {
    expect(Validators.isValidPin('1234'), isTrue);
    expect(Validators.isValidPin('123456'), isTrue);
    expect(Validators.isValidPin('123'), isFalse);
    expect(Validators.isValidPin('1234567'), isFalse);
    expect(Validators.isValidPin('12a4'), isFalse);
  });

  test('ReminderConfig encode/decode/toggle is robust', () {
    const c = ReminderConfig([10080, 1440, 1440, -1]);
    final round = ReminderConfig.decode(c.encode());
    expect(round.offsetsMinutes, [1440, 10080]);
    expect(ReminderConfig.decode('not json').isEnabled, isFalse);
    expect(ReminderConfig.decode(null).isEnabled, isFalse);
    expect(round.toggle(1440).offsetsMinutes, [10080]);
    expect(round.toggle(4320).offsetsMinutes, [1440, 4320, 10080]);
  });

  test('TaskItem tolerates bad priority values from old/foreign data', () {
    final t = TaskItem.fromMap({'id': 'x', 'title': 'T', 'priority': 99, 'created_at': 0, 'updated_at': 0});
    expect(t.priority, TaskPriority.high);
  });

  test('LIKE pattern escaping', () {
    expect(likePattern('50%_off'), r'%50\%\_off%');
  });

  test('attachment names cannot escape the folder', () {
    expect(AttachmentService.isSafeName('abc123.jpg'), isTrue);
    expect(AttachmentService.isSafeName('../etc/passwd'), isFalse);
    expect(AttachmentService.isSafeName('a/b.jpg'), isFalse);
    expect(AttachmentService.isSafeName('.hidden'), isFalse);
  });
}
