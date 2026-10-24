
export default function Footer() {
  return (
    <footer className="border-t border-slate-800 bg-slate-950 px-6 py-4 text-center text-xs text-slate-500 sm:flex sm:items-center sm:justify-between sm:text-left">
      <p>
        © {new Date().getFullYear()} IncidentOps
      </p>

      <p className="mt-1 sm:mt-0">
        Self-Hosted Incident Management & Infrastructure Monitoring
      </p>
    </footer>
  )
}
