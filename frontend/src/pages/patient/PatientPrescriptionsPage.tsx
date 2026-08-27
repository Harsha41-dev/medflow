import { useEffect, useState } from "react";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";
import type { PageResponse, Prescription } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";

export function PatientPrescriptionsPage() {
  const { auth } = useAuth();
  const [prescriptionsPage, setPrescriptionsPage] = useState<PageResponse<Prescription> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadPrescriptions() {
      if (!auth?.profileId) {
        setError("Patient profile was not found for this account.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await clinicalApi.listPatientPrescriptions(auth.profileId, page, 10);
        setPrescriptionsPage(data);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadPrescriptions();
  }, [auth?.profileId, page]);

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Prescriptions</p>
        <h1>My Prescriptions</h1>
        <p>Review prescriptions recorded during your encounters.</p>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading prescriptions" /> : null}

      {!loading && prescriptionsPage?.content.length === 0 ? <div className="empty-state">No prescriptions found.</div> : null}

      {!loading && prescriptionsPage && prescriptionsPage.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Medication</th>
                  <th>Dosage</th>
                  <th>Frequency</th>
                  <th>Duration</th>
                  <th>Instructions</th>
                </tr>
              </thead>
              <tbody>
                {prescriptionsPage.content.map((prescription) => (
                  <tr key={prescription.id}>
                    <td>{prescription.medicationName}</td>
                    <td>{prescription.dosage}</td>
                    <td>{prescription.frequency}</td>
                    <td>{prescription.duration}</td>
                    <td>{prescription.instructions || "Not provided"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={page} totalPages={prescriptionsPage.totalPages} onPageChange={setPage} />
        </>
      ) : null}
    </section>
  );
}
