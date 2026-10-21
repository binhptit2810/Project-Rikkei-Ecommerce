import React from 'react'

export const Footer = () => {
  return (
    <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500 mt-auto">
      <div className="max-w-6xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>E-commerce Platform — Base Project for Students • React + Spring Boot + MySQL</p>
        <p className="text-slate-400">
          Người phụ trách Auth & User: <span className="text-slate-600 font-medium">Cao Chí Thiện</span>
        </p>
      </div>
    </footer>
  )
}

export default Footer
