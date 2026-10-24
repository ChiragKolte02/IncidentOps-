
// import { useState } from 'react'
// import Navbar from './Navbar'
// import Sidebar from './Sidebar'
// import Footer from './Footer'

// export default function DashboardLayout({
//   title,
//   description,
//   children,
// }) {
//   const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

//   return (
//     <div className="flex min-h-screen flex-col bg-slate-950 text-white">
//       <Navbar />

//       <div className="flex flex-1">
//         <div className="hidden md:flex">
//           <Sidebar />
//         </div>

//         <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">
//           <div className="mb-6 flex items-center gap-3 md:hidden">
//             <button
//               type="button"
//               onClick={() => setMobileMenuOpen((open) => !open)}
//               aria-expanded={mobileMenuOpen}
//               className="rounded-lg border border-slate-700 px-3 py-2 text-sm transition-colors duration-200 hover:bg-slate-800"
//             >
//               {mobileMenuOpen ? 'Close menu' : '☰ Menu'}
//             </button>
//           </div>

//           {/* Smooth mobile sidebar */}
//           <div
//             className={`grid transition-[grid-template-rows,opacity,margin] duration-300 ease-in-out md:hidden ${
//               mobileMenuOpen
//                 ? 'mb-6 grid-rows-[1fr] opacity-100'
//                 : 'mb-0 grid-rows-[0fr] opacity-0'
//             }`}
//             aria-hidden={!mobileMenuOpen}
//           >
//             <div className="overflow-hidden">
//               <div
//                 className={`origin-top transform transition-transform duration-300 ease-in-out ${
//                   mobileMenuOpen
//                     ? 'translate-y-0 scale-y-100'
//                     : '-translate-y-2 scale-y-95'
//                 }`}
//                 inert={!mobileMenuOpen}
//               >
//                 <Sidebar
//                   onNavigate={() => setMobileMenuOpen(false)}
//                 />
//               </div>
//             </div>
//           </div>

//           <div id="overview" className="mb-8 scroll-mt-6">
//             <p className="text-xs font-semibold uppercase tracking-widest text-blue-400">
//               IncidentOps Workspace
//             </p>

//             <h2 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">
//               {title}
//             </h2>

//             {description && (
//               <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">
//                 {description}
//               </p>
//             )}
//           </div>

//           {children}
//         </main>
//       </div>

//       <Footer />
//     </div>
//   )
// }

import { useState } from 'react'
import Navbar from './Navbar'
import Sidebar from './Sidebar'
import Footer from './Footer'

export default function DashboardLayout({
  title,
  description,
  children,
}) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [desktopSidebarOpen, setDesktopSidebarOpen] = useState(true)

  return (
    <div className="flex min-h-screen flex-col bg-slate-950 text-white">
      <Navbar
        desktopSidebarOpen={desktopSidebarOpen}
        onToggleDesktopSidebar={() => setDesktopSidebarOpen((o) => !o)}
      />

      <div className="flex flex-1">
        {/* Smooth desktop sidebar — animates width */}
        <aside
          className={`hidden shrink-0 overflow-hidden transition-[width,opacity] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] will-change-[width,opacity] md:block ${
            desktopSidebarOpen
              ? 'w-64 opacity-100'
              : 'w-0 opacity-0'
          }`}
          aria-hidden={!desktopSidebarOpen}
          inert={!desktopSidebarOpen}
        >
          <div className="h-full w-64">
            <Sidebar />
          </div>
        </aside>

        <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">
          {/* Mobile menu toggle */}
          <div className="mb-6 flex items-center gap-3 md:hidden">
            <button
              type="button"
              onClick={() => setMobileMenuOpen((open) => !open)}
              aria-expanded={mobileMenuOpen}
              aria-controls="mobile-sidebar"
              className="rounded-lg border border-slate-700 px-3 py-2 text-sm transition-colors duration-200 hover:bg-slate-800"
            >
              {mobileMenuOpen ? 'Close menu' : '☰ Menu'}
            </button>
          </div>

          {/* Smooth mobile sidebar — height + fade + slide */}
          <div
            id="mobile-sidebar"
            className={`grid transition-[grid-template-rows,opacity,margin-bottom] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] will-change-[grid-template-rows,opacity] md:hidden ${
              mobileMenuOpen
                ? 'mb-6 grid-rows-[1fr] opacity-100'
                : 'mb-0 grid-rows-[0fr] opacity-0'
            }`}
            aria-hidden={!mobileMenuOpen}
          >
            <div className="overflow-hidden">
              <div
                className={`origin-top transform-gpu transition-transform duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] ${
                  mobileMenuOpen
                    ? 'translate-y-0'
                    : '-translate-y-3'
                }`}
                inert={!mobileMenuOpen}
              >
                <Sidebar
                  onNavigate={() => setMobileMenuOpen(false)}
                />
              </div>
            </div>
          </div>

          <div id="overview" className="mb-8 scroll-mt-6">
            <p className="text-xs font-semibold uppercase tracking-widest text-blue-400">
              IncidentOps Workspace
            </p>

            <h2 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">
              {title}
            </h2>

            {description && (
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">
                {description}
              </p>
            )}
          </div>

          {children}
        </main>
      </div>

      <Footer />
    </div>
  )
}
