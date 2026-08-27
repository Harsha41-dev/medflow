export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

export function normalizeDateTimeForBackend(value: string): string {
  if (value.length === 16) {
    return value + ":00";
  }
  return value;
}

export function isThirtyMinuteSlot(value: string): boolean {
  if (!value) {
    return false;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return false;
  }

  const minutes = date.getMinutes();
  return minutes === 0 || minutes === 30;
}

export function isFutureDateTime(value: string): boolean {
  if (!value) {
    return false;
  }

  const date = new Date(value);
  return date.getTime() > Date.now();
}
