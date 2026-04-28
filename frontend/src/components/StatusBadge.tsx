import type { AppointmentStatus } from "../types";

interface StatusBadgeProps {
  status: AppointmentStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return <span className={"status-badge status-" + status.toLowerCase()}>{status}</span>;
}
