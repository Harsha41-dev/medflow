import type { Role } from "../types";

export function dashboardPathForRole(role: Role): string {
  if (role === "PATIENT") {
    return "/patient/dashboard";
  }

  if (role === "DOCTOR") {
    return "/doctor/dashboard";
  }

  return "/admin/dashboard";
}
