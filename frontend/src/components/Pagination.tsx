interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="pagination" aria-label="Pagination">
      <button type="button" className="button button-secondary" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="button button-secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </button>
    </div>
  );
}
