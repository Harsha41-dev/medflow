import { useEffect, useState } from "react";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";
import type { Encounter, PageResponse } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

export function PatientMedicalHistoryPage() {
  const { auth } = useAuth();
  const [encountersPage, setEncountersPage] = useState<PageResponse<Encounter> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadHistory() {
      if (!auth?.profileId) {
        setError("Patient profile was not found for this account.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await clinicalApi.listPatientEncounters(auth.profileId, page, 10);
        setEncountersPage(data);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadHistory();
  }, [auth?.profileId, page]);

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Medical history</p>
        <h1>Encounter History</h1>
        <p>View visit notes shared through completed clinical encounters.</p>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading medical history" /> : null}

      {!loading && encountersPage?.content.length === 0 ? <div className="empty-state">No encounters found.</div> : null}

      {!loading && encountersPage && encountersPage.content.length > 0 ? (
        <>
          <div className="list-stack">
            {encountersPage.content.map((encounter) => (
              <article key={encounter.id} className="record-card">
                <div>
                  <h2>{encounter.chiefComplaint}</h2>
                  <p>Doctor: {encounter.doctorName}</p>
                  <p>Visit: {formatDateTime(encounter.visitDate)}</p>
                  {encounter.notes ? <p>{encounter.notes}</p> : null}
                </div>
              </article>
            ))}
          </div>

          <Pagination page={page} totalPages={encountersPage.totalPages} onPageChange={setPage} />
        </>
      ) : null}
    </section>
  );
}
