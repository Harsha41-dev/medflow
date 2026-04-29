import { type FormEvent, useEffect, useState } from "react";
import { adminApi } from "../../api/adminApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import type { AuditAction, AuditLog, PageResponse } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

const auditActions: AuditAction[] = [
  "USER_LOGIN_SUCCESS",
  "USER_LOGIN_FAILED",
  "PATIENT_VIEWED",
  "PATIENT_UPDATED",
  "DOCTOR_CREATED",
  "APPOINTMENT_CREATED",
  "APPOINTMENT_VIEWED",
  "APPOINTMENT_CANCELLED",
  "ENCOUNTER_CREATED",
  "ENCOUNTER_VIEWED",
  "DIAGNOSIS_CREATED",
  "PRESCRIPTION_CREATED",
  "PRESCRIPTION_VIEWED",
  "SYMPTOM_PATTERN_PREDICTED"
];

export function AdminAuditLogsPage() {
  const [logsPage, setLogsPage] = useState<PageResponse<AuditLog> | null>(null);
  const [action, setAction] = useState<AuditAction | "">("");
  const [userId, setUserId] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadLogs();
  }, [page]);

  async function loadLogs() {
    try {
      setLoading(true);
      setError("");
      const data = await adminApi.listAuditLogs({ action, userId, page, size: 10 });
      setLogsPage(data);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setLoading(false);
    }
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (page === 0) {
      loadLogs();
    } else {
      setPage(0);
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Audit logs</p>
        <h1>Audit Activity</h1>
        <p>View action metadata recorded by the backend audit service.</p>
      </div>

      <form className="toolbar" onSubmit={applyFilters}>
        <label className="field compact-field">
          <span>Action</span>
          <select value={action} onChange={(event) => setAction(event.target.value as AuditAction | "")}>
            <option value="">All</option>
            {auditActions.map((auditAction) => (
              <option key={auditAction} value={auditAction}>
                {auditAction}
              </option>
            ))}
          </select>
        </label>

        <label className="field compact-field">
          <span>User ID</span>
          <input value={userId} onChange={(event) => setUserId(event.target.value)} inputMode="numeric" />
        </label>

        <button type="submit" className="button button-primary">
          Apply filters
        </button>
      </form>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading audit logs" /> : null}

      {!loading && logsPage?.content.length === 0 ? <div className="empty-state">No audit logs found.</div> : null}

      {!loading && logsPage && logsPage.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Entity type</th>
                  <th>Entity ID</th>
                </tr>
              </thead>
              <tbody>
                {logsPage.content.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.timestamp)}</td>
                    <td>
                      <span>{log.userEmail}</span>
                      <span className="muted-text">ID {log.userId}</span>
                    </td>
                    <td>{log.action}</td>
                    <td>{log.entityType}</td>
                    <td>{log.entityId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={page} totalPages={logsPage.totalPages} onPageChange={setPage} />
        </>
      ) : null}
    </section>
  );
}
