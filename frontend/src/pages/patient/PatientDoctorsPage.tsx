import { type FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { doctorApi } from "../../api/doctorApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import type { Doctor, PageResponse } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";

export function PatientDoctorsPage() {
  const [doctorsPage, setDoctorsPage] = useState<PageResponse<Doctor> | null>(null);
  const [specialization, setSpecialization] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDoctors(page);
  }, [page]);

  async function loadDoctors(nextPage: number) {
    try {
      setLoading(true);
      setError("");
      const data = await doctorApi.listDoctors({ specialization, page: nextPage, size: 8 });
      setDoctorsPage(data);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setLoading(false);
    }
  }

  function handleFilter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (page === 0) {
      loadDoctors(0);
    } else {
      setPage(0);
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Doctor listing</p>
        <h1>Find a Doctor</h1>
        <p>Browse active doctors and choose one for appointment booking.</p>
      </div>

      <form className="toolbar" onSubmit={handleFilter}>
        <label className="field compact-field">
          <span>Specialization</span>
          <input value={specialization} onChange={(event) => setSpecialization(event.target.value)} placeholder="Dermatology" />
        </label>
        <button type="submit" className="button button-primary">
          Search
        </button>
      </form>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading doctors" /> : null}

      {!loading && doctorsPage?.content.length === 0 ? <div className="empty-state">No doctors found.</div> : null}

      {!loading && doctorsPage && doctorsPage.content.length > 0 ? (
        <>
          <div className="cards-grid">
            {doctorsPage.content.map((doctor) => (
              <article key={doctor.id} className="record-card">
                <div>
                  <h2>
                    Dr. {doctor.firstName} {doctor.lastName}
                  </h2>
                  <p>{doctor.specialization}</p>
                  <p>License: {doctor.licenseNumber}</p>
                </div>
                <Link className="button button-primary" to={"/patient/book-appointment?doctorId=" + doctor.id}>
                  Book
                </Link>
              </article>
            ))}
          </div>

          <Pagination page={page} totalPages={doctorsPage.totalPages} onPageChange={setPage} />
        </>
      ) : null}
    </section>
  );
}
