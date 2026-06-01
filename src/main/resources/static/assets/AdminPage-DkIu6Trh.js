import{j as e}from"./index-BexZMbGE.js";import{u as i}from"./index-Z7pE7Zxz.js";function l({title:t,subtitle:a,actions:r}){return e.jsxs(e.Fragment,{children:[e.jsxs("header",{className:"page-header animate-fade-in",children:[e.jsxs("div",{children:[e.jsx("h1",{className:"page-title",children:t}),a&&e.jsx("p",{className:"page-subtitle",children:a})]}),r&&e.jsx("div",{className:"page-header-actions",children:r})]}),e.jsx("style",{children:`
        .page-header {
          display: flex; align-items: flex-start;
          justify-content: space-between; gap: 16px;
          margin-bottom: 24px; flex-wrap: wrap;
        }
        .page-title {
          font-family: var(--font-display);
          font-size: 1.75rem; font-weight: 800; color: var(--text-0);
        }
        .page-subtitle { font-size: 0.82rem; color: var(--text-2); margin-top: 3px; }
        .page-header-actions {
          display: flex; gap: 8px; align-items: center; flex-shrink: 0;
        }
        .badge {
          display: inline-flex; align-items: center;
          font-family: var(--font-mono); font-size: 0.68rem; font-weight: 700;
          letter-spacing: 0.07em; text-transform: uppercase;
          padding: 3px 8px; border-radius: 100px; border: 1px solid;
          white-space: nowrap;
        }
      `})]})}function d(){return i("Admin"),e.jsxs(e.Fragment,{children:[e.jsx(l,{title:"Panel Administratora",subtitle:"Knowledge-Forge — zarządzanie platformą"}),e.jsxs("div",{className:"adm-placeholder",children:[e.jsx("span",{className:"adm-placeholder-icon",children:"⚙"}),e.jsx("h2",{className:"adm-placeholder-title",children:"Panel w budowie"}),e.jsx("p",{className:"adm-placeholder-desc",children:"Funkcje administracyjne zostaną dodane wraz z rozwojem platformy."})]}),e.jsx("style",{children:`
        .adm-placeholder {
          background: var(--bg-1);
          border: 1px solid var(--border-1);
          border-radius: var(--radius-lg);
          padding: 80px 40px;
          text-align: center;
          display: flex; flex-direction: column; align-items: center; gap: 12px;
          margin-top: 8px;
        }
        .adm-placeholder-icon {
          font-size: 2.5rem; color: var(--cyan); opacity: 0.5; margin-bottom: 8px;
        }
        .adm-placeholder-title {
          font-size: 1.1rem; font-weight: 700; color: var(--text-1);
        }
        .adm-placeholder-desc {
          font-size: 0.83rem; color: var(--text-2);
          max-width: 420px; line-height: 1.7;
        }
      `})]})}export{d as default};
