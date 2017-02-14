'use strict';

// http://stackoverflow.com/a/38676384/1127848
//
class DateFormat {

  get monthNames() {
    return [
      'January', 'February', 'March',
      'April', 'May', 'June', 'July',
      'August', 'September', 'October',
      'November', 'December'
    ];
  }

  get days() {
    return [
      'Sunday', 'Monday', 'Tuesday', 'Wednesday',
      'Thursday', 'Friday', 'Saturday'
    ];
  }

  pad(n, width, z) {
    z = z || '0';
    n = n + '';
    return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
  }

  format(dt, format) {
    format = format.replace('ss', this.pad(dt.getSeconds(), 2));
    format = format.replace('s', dt.getSeconds());
    format = format.replace('dd', this.pad(dt.getDate(), 2));
    format = format.replace('d', dt.getDate());
    format = format.replace('mm', this.pad(dt.getMinutes(), 2));
    format = format.replace('m', dt.getMinutes());
    format = format.replace('MMMM', this.monthNames[dt.getMonth()]);
    format = format.replace('MMM', this.monthNames[dt.getMonth()].substring(0, 3));
    format = format.replace('MM', this.pad(dt.getMonth() + 1, 2));
    format = format.replace('M', dt.getMonth() + 1);
    format = format.replace('DD', this.days[dt.getDay()]);
    format = format.replace(/D(?!e)/, this.days[dt.getDay()].substring(0, 3));
    format = format.replace('yyyy', dt.getFullYear());
    format = format.replace('YYYY', dt.getFullYear());
    format = format.replace('yy', (dt.getFullYear() + '').substring(2));
    format = format.replace('YY', (dt.getFullYear() + '').substring(2));
    format = format.replace('HH', this.pad(dt.getHours(), 2));
    format = format.replace('H', dt.getHours());
    return format;
  }
}

var formatter = new DateFormat();

module.exports = function(dt, format) {
  return formatter.format(dt, format);
};
