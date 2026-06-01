import{j as e,r as i,t as N}from"./index-DtuzPm3T.js";import{u as W}from"./index-D54JKurE.js";import{n as R,t as S,c as T}from"./services-D6-2Q08e.js";const O=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M3.5 2L7 5.5 3.5 9",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),K=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M2 3.5L5.5 7 9 3.5",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),$=()=>e.jsxs("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1",width:"9",height:"10",rx:"1.5",stroke:"currentColor",strokeWidth:"1.3"}),e.jsx("path",{d:"M3.5 4h5M3.5 6.5h5M3.5 9h3",stroke:"currentColor",strokeWidth:"1.2",strokeLinecap:"round"})]}),z=()=>e.jsx("svg",{width:"10",height:"10",viewBox:"0 0 10 10",fill:"none",children:e.jsx("path",{d:"M5 1.5v7M1.5 5h7",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),U={BASIC:"#16a34a",MEDIUM:"#d97706",HARD:"#dc2626"};function P({node:a,depth:r=0,expanded:o,topics:l,loadingTopics:c,selectedTopicId:u,onToggle:x,onSelectTopic:m,onAddSubcategory:h,onAddTopic:g}){const[j,p]=i.useState(!1),b=o.has(a.id),f=l[a.id]??[],v=c.has(a.id),y=r===0,t=r===1;return e.jsxs("div",{className:`tn-block tn-block--d${r}`,children:[e.jsxs("div",{className:`tn-row tn-row--d${r}${b?" tn-row--open":""}`,onMouseEnter:()=>p(!0),onMouseLeave:()=>p(!1),children:[e.jsx("button",{className:"tn-chevron",onClick:()=>x(a.id),children:b?e.jsx(K,{}):e.jsx(O,{})}),e.jsx("span",{className:"tn-name",onClick:()=>x(a.id),title:a.name,children:a.name}),j&&e.jsxs("div",{className:"tn-actions",children:[y&&e.jsxs("button",{className:"tn-act tn-act--sub",title:"Dodaj podkategorię",onClick:s=>{s.stopPropagation(),h(a.id,a.name)},children:[e.jsx(z,{})," ",e.jsx("span",{children:"Podkat."})]}),t&&e.jsxs("button",{className:"tn-act tn-act--topic",title:"Dodaj temat / notatkę",onClick:s=>{s.stopPropagation(),g(a.id,a.name)},children:[e.jsx(z,{})," ",e.jsx("span",{children:"Temat"})]}),!y&&!t&&e.jsxs(e.Fragment,{children:[e.jsxs("button",{className:"tn-act tn-act--sub",onClick:s=>{s.stopPropagation(),h(a.id,a.name)},children:[e.jsx(z,{})," ",e.jsx("span",{children:"Podkat."})]}),e.jsxs("button",{className:"tn-act tn-act--topic",onClick:s=>{s.stopPropagation(),g(a.id,a.name)},children:[e.jsx(z,{})," ",e.jsx("span",{children:"Temat"})]})]})]})]}),b&&e.jsxs("div",{className:"tn-children",children:[(a.children??[]).map(s=>e.jsx(P,{node:s,depth:r+1,expanded:o,topics:l,loadingTopics:c,selectedTopicId:u,onToggle:x,onSelectTopic:m,onAddSubcategory:h,onAddTopic:g},s.id)),v&&e.jsxs("div",{className:"tn-loading",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}),!v&&f.map(s=>e.jsxs("button",{className:`tn-topic${u===s.id?" tn-topic--active":""}`,onClick:()=>m(s),children:[e.jsx("span",{className:"tn-topic-icon",children:e.jsx($,{})}),e.jsx("span",{className:"tn-topic-name",title:s.title,children:s.title}),e.jsx("span",{className:"tn-topic-diff",title:s.difficulty,style:{background:U[s.difficulty]}})]},s.id)),!v&&f.length===0&&(a.children??[]).length===0&&e.jsx("p",{className:"tn-empty-hint",children:y?"Brak podkategorii":"Brak tematów"})]})]})}function H({tree:a,expanded:r,topics:o,loadingTopics:l,selectedTopicId:c,onToggle:u,onSelectTopic:x,onAddRootCategory:m,onAddSubcategory:h,onAddTopic:g,loading:j}){return e.jsxs("aside",{className:"tree-panel",children:[e.jsxs("div",{className:"tree-head",children:[e.jsx("span",{className:"tree-head-title",children:"Baza wiedzy"}),e.jsx("button",{className:"tree-add-btn",onClick:m,title:"Nowa kategoria (np. IT, HISTORIA)",children:e.jsx(z,{})})]}),e.jsxs("div",{className:"tree-legend",children:[e.jsx("span",{className:"tree-legend-item tree-legend-item--cat",children:"Kategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--sub",children:"Podkategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--topic",children:"Temat"})]}),e.jsx("div",{className:"tree-body",children:j?e.jsxs("div",{className:"tree-status",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}):a.length===0?e.jsxs("div",{className:"tree-empty",children:[e.jsx("svg",{width:"36",height:"36",viewBox:"0 0 36 36",fill:"none",style:{color:"var(--border-1)"},children:e.jsx("path",{d:"M4 10a2 2 0 012-2h8l2 2h14a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V10z",stroke:"currentColor",strokeWidth:"1.5"})}),e.jsx("p",{className:"tree-empty-title",children:"Brak kategorii"}),e.jsxs("p",{className:"tree-empty-sub",children:["Zacznij od dodania kategorii",e.jsx("br",{}),"np. ",e.jsx("em",{children:"IT"})," lub ",e.jsx("em",{children:"HISTORIA"})]}),e.jsx("button",{className:"tree-empty-btn",onClick:m,children:"Dodaj kategorię →"})]}):a.map(p=>e.jsx(P,{node:p,depth:0,expanded:r,topics:o,loadingTopics:l,selectedTopicId:c,onToggle:u,onSelectTopic:x,onAddSubcategory:h,onAddTopic:g},p.id))}),e.jsx("style",{children:`
        .tree-panel {
          width: 268px; flex-shrink: 0;
          border-right: 1px solid var(--border-1);
          background: var(--bg-2);
          display: flex; flex-direction: column;
          height: 100%;
        }

        /* Nagłówek */
        .tree-head {
          display: flex; align-items: center; justify-content: space-between;
          padding: 13px 14px 10px;
          border-bottom: 1px solid var(--border-1);
          background: #fff;
          flex-shrink: 0;
        }
        .tree-head-title {
          font-size: 0.68rem; font-weight: 800;
          letter-spacing: 0.12em; text-transform: uppercase;
          color: var(--text-2);
        }
        .tree-add-btn {
          width: 24px; height: 24px; border-radius: 6px;
          background: var(--accent); border: none;
          color: #fff; display: flex; align-items: center; justify-content: center;
          cursor: pointer; transition: background var(--t-fast);
        }
        .tree-add-btn:hover { background: var(--accent-dim); }

        /* Legenda */
        .tree-legend {
          display: flex; align-items: center; gap: 4px;
          padding: 6px 14px 8px;
          border-bottom: 1px solid var(--border-1);
          background: #fff;
          flex-shrink: 0;
        }
        .tree-legend-item {
          font-family: var(--font-mono); font-size: 0.58rem;
          font-weight: 700; letter-spacing: 0.07em; text-transform: uppercase;
        }
        .tree-legend-item--cat   { color: var(--accent); }
        .tree-legend-item--sub   { color: var(--text-2); }
        .tree-legend-item--topic { color: var(--text-3); }
        .tree-legend-sep { color: var(--text-3); font-size: 0.65rem; }

        /* Ciało */
        .tree-body {
          flex: 1; overflow-y: auto;
          padding: 8px 0 16px;
        }

        /* ── Blok węzła ── */
        .tn-block { }

        /* ── Wiersz ── */
        .tn-row {
          display: flex; align-items: center; gap: 5px;
          cursor: default; min-height: 32px;
          transition: background var(--t-fast);
          padding-right: 8px;
          position: relative;
        }
        .tn-row:hover { background: rgba(0,0,0,0.03); }

        /* Depth 0 — Kategoria (IT, HISTORIA) */
        .tn-row--d0 {
          padding-left: 10px;
          border-bottom: 1px solid var(--border-2);
          margin-top: 4px;
        }
        .tn-row--d0:first-child { margin-top: 0; }
        .tn-row--d0 .tn-name {
          font-size: 0.78rem; font-weight: 800;
          text-transform: uppercase; letter-spacing: 0.06em;
          color: var(--text-0);
        }

        /* Depth 1 — Podkategoria (JAVA, SIECI) */
        .tn-row--d1 { padding-left: 22px; }
        .tn-row--d1 .tn-name {
          font-size: 0.80rem; font-weight: 600;
          color: var(--text-1);
        }

        /* Depth 2+ */
        .tn-row--d2 { padding-left: 38px; }
        .tn-row--d2 .tn-name {
          font-size: 0.79rem; font-weight: 500;
          color: var(--text-2);
        }

        .tn-chevron {
          width: 18px; height: 18px; flex-shrink: 0;
          background: none; border: none; cursor: pointer;
          color: var(--text-3);
          display: flex; align-items: center; justify-content: center;
          border-radius: 4px; transition: color var(--t-fast), background var(--t-fast);
        }
        .tn-chevron:hover { color: var(--accent); background: var(--accent-light); }

        .tn-name {
          flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
          cursor: pointer; transition: color var(--t-fast);
        }
        .tn-name:hover { color: var(--accent) !important; }

        /* Akcje hover */
        .tn-actions {
          display: flex; gap: 3px; flex-shrink: 0;
        }
        .tn-act {
          display: flex; align-items: center; gap: 2px;
          padding: 3px 7px; border-radius: 5px;
          font-size: 0.64rem; font-weight: 700;
          cursor: pointer; white-space: nowrap;
          border: 1px solid; transition: all var(--t-fast);
        }
        .tn-act--sub {
          background: var(--accent-light); color: var(--accent);
          border-color: var(--border-0);
        }
        .tn-act--sub:hover { background: var(--accent-glow-strong); }
        .tn-act--topic {
          background: #f0fdf4; color: #16a34a;
          border-color: rgba(22,163,74,.25);
        }
        .tn-act--topic:hover { background: #dcfce7; }

        /* Dzieci */
        .tn-children { }

        /* Temat (liść) */
        .tn-topic {
          display: flex; align-items: center; gap: 8px;
          width: 100%; padding: 6px 10px 6px 46px;
          background: none; border: none; text-align: left;
          cursor: pointer; min-height: 28px;
          transition: background var(--t-fast);
          border-left: 2px solid transparent;
        }
        .tn-topic:hover { background: rgba(37,99,235,0.04); }
        .tn-topic--active {
          background: var(--accent-light) !important;
          border-left-color: var(--accent);
        }
        .tn-topic--active .tn-topic-name { color: var(--accent); font-weight: 600; }
        .tn-topic-icon { color: var(--text-3); flex-shrink: 0; display: flex; }
        .tn-topic-name {
          flex: 1; font-size: 0.78rem; color: var(--text-1);
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
          transition: color var(--t-fast);
        }
        .tn-topic-diff {
          width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
        }

        /* Stany */
        .tn-loading {
          display: flex; align-items: center; gap: 6px;
          font-size: 0.74rem; color: var(--text-3); padding: 6px 46px;
        }
        .tn-spinner {
          width: 10px; height: 10px; border-radius: 50%;
          border: 1.5px solid var(--border-1); border-top-color: var(--accent);
          animation: spin .6s linear infinite; flex-shrink: 0;
        }
        .tn-empty-hint {
          font-size: 0.71rem; color: var(--text-3); padding: 4px 46px;
          font-style: italic;
        }

        /* Puste drzewo */
        .tree-status {
          display: flex; align-items: center; gap: 8px;
          padding: 20px 14px; font-size: 0.8rem; color: var(--text-2);
        }
        .tree-empty {
          padding: 28px 16px; text-align: center;
        }
        .tree-empty svg { margin: 0 auto 12px; }
        .tree-empty-title { font-size: 0.88rem; font-weight: 700; color: var(--text-2); margin-bottom: 6px; }
        .tree-empty-sub   { font-size: 0.76rem; color: var(--text-3); line-height: 1.6; margin-bottom: 14px; }
        .tree-empty-sub em { font-style: normal; color: var(--accent); font-weight: 600; }
        .tree-empty-btn {
          font-size: 0.78rem; font-weight: 700; color: var(--accent);
          background: var(--accent-light); border: 1px solid var(--border-0);
          padding: 7px 14px; border-radius: var(--radius-md); cursor: pointer;
          transition: background var(--t-fast);
        }
        .tree-empty-btn:hover { background: var(--accent-glow-strong); }
      `})]})}function F({topicTitle:a,onClose:r}){return e.jsxs("div",{className:"modal-backdrop",onClick:r,children:[e.jsxs("div",{className:"modal-box",onClick:o=>o.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:"AI · Dopytaj o temat"}),e.jsx("h2",{className:"modal-title",children:a})]}),e.jsx("button",{className:"modal-close",onClick:r,children:"✕"})]}),e.jsxs("div",{className:"modal-body",children:[e.jsx("p",{className:"modal-info",children:"Zadaj dodatkowe pytanie dotyczące tej notatki. AI uzupełni lub rozszerzy treść na podstawie Twojego pytania."}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Twoje pytanie"}),e.jsx("textarea",{className:"mf-input mf-textarea",placeholder:"np. Wyjaśnij prościej, dodaj przykład z życia codziennego, pokaż jak to działa w Spring Boot...",rows:4,disabled:!0})]}),e.jsxs("div",{className:"modal-placeholder",children:[e.jsx("span",{className:"modal-placeholder-icon",children:"🔧"}),e.jsx("span",{children:"Integracja AI w przygotowaniu"})]})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{className:"btn-ghost",onClick:r,children:"Zamknij"}),e.jsx("button",{className:"btn-primary",disabled:!0,title:"Wkrótce dostępne",children:"Wyślij pytanie →"})]})]}),e.jsx("style",{children:`
        .modal-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .modal-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:460px;
          animation:fadeIn .2s ease both;
        }
        .modal-head { display:flex; align-items:flex-start; justify-content:space-between; padding:20px 20px 0; gap:12px; }
        .modal-eyebrow { font-family:var(--font-mono); font-size:0.62rem; font-weight:700; letter-spacing:0.1em; text-transform:uppercase; color:var(--accent); margin-bottom:3px; }
        .modal-title { font-size:0.95rem; font-weight:700; color:var(--text-0); }
        .modal-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; flex-shrink:0; margin-top:-2px; transition:background var(--t-fast); }
        .modal-close:hover { background:var(--bg-2); }
        .modal-body { padding:16px 20px; display:flex; flex-direction:column; gap:12px; }
        .modal-info { font-size:0.8rem; color:var(--text-2); line-height:1.6; }
        .modal-placeholder { display:flex; align-items:center; gap:8px; padding:10px 14px; background:var(--yellow-light); border:1px solid rgba(217,119,6,.2); border-radius:var(--radius-md); font-size:0.77rem; color:var(--yellow); font-weight:600; }
        .modal-placeholder-icon { font-size:1rem; }
        .modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:0 20px 20px; }
        .mf-field { display:flex; flex-direction:column; gap:5px; }
        .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
        .mf-input { padding:10px 12px; border-radius:var(--radius-md); border:1.5px solid var(--border-1); background:var(--bg-2); font-size:0.88rem; color:var(--text-0); outline:none; transition:border-color .15s; resize:vertical; }
        .mf-input:disabled { opacity:.5; cursor:not-allowed; }
        .mf-textarea { font-family:inherit; min-height:80px; }
        .btn-primary { padding:9px 20px; background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700; cursor:pointer; transition:background .15s; }
        .btn-primary:disabled { opacity:.4; cursor:not-allowed; }
        .btn-ghost { padding:9px 16px; background:transparent; color:var(--text-2); border:1px solid var(--border-1); border-radius:var(--radius-md); font-size:0.84rem; cursor:pointer; transition:background .15s; }
        .btn-ghost:hover { background:var(--bg-2); }
      `})]})}const I=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}];function Z({topic:a,categoryPath:r,onTopicUpdated:o}){const[l,c]=i.useState(null),[u,x]=i.useState(!1),[m,h]=i.useState(a?.difficulty??"BASIC"),[g,j]=i.useState(!1),[p,b]=i.useState(!1),[f,v]=i.useState(!1);i.useEffect(()=>{if(!a){c(null);return}h(a.difficulty),x(!0),R.getByTopic(a.id).then(t=>c(t.data)).catch(t=>{t.status===404?c(null):N.error("Błąd ładowania notatki")}).finally(()=>x(!1))},[a?.id]);const y=async t=>{if(!(t===m||p)){h(t),b(!0);try{const s=await S.update(a.id,{difficulty:t});o?.(s.data)}catch{N.error("Błąd zapisu poziomu trudności"),h(a.difficulty)}finally{b(!1)}}};return a?(I.find(t=>t.value===m),e.jsxs("div",{className:"note-panel",children:[e.jsxs("div",{className:"note-header",children:[e.jsx("div",{className:"note-breadcrumb",children:r?.map((t,s)=>e.jsxs("span",{children:[s>0&&e.jsx("span",{className:"note-bc-sep",children:"›"}),e.jsx("span",{className:"note-bc-item",children:t.name})]},t.id))}),e.jsx("h1",{className:"note-title",children:a.title}),e.jsx("div",{className:"note-meta",children:e.jsx("span",{className:"note-status-badge","data-status":a.status,children:Y[a.status]??a.status})})]}),e.jsxs("div",{className:"note-config",children:[e.jsxs("div",{className:"note-config-row",children:[e.jsx("span",{className:"note-config-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:I.map(t=>e.jsxs("button",{className:`diff-btn${m===t.value?" diff-btn--on":""}`,style:m===t.value?{background:t.bg,borderColor:t.border,color:t.color}:{},onClick:()=>y(t.value),disabled:p,children:[t.label,m===t.value&&p&&e.jsx("span",{className:"diff-saving"})]},t.value))})]}),e.jsx("div",{className:"note-config-row",children:e.jsxs("label",{className:"code-check",children:[e.jsx("input",{type:"checkbox",checked:g,onChange:t=>j(t.target.checked),className:"code-check-input"}),e.jsx("span",{className:"code-check-box",children:g&&e.jsx("svg",{width:"10",height:"10",viewBox:"0 0 10 10",fill:"none",children:e.jsx("path",{d:"M1.5 5.5L4 8l4.5-5.5",stroke:"white",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})})}),e.jsxs("span",{className:"code-check-label",children:[e.jsx("span",{className:"code-check-text",children:"Czy to KOD?"}),e.jsx("span",{className:"code-check-sub",children:"Zaznacz jeśli notatka dotyczy kodu / programowania"})]}),g&&e.jsxs("span",{className:"code-check-badge",children:[e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M7 2l2.5 3.5L7 9M4 2L1.5 5.5 4 9",stroke:"currentColor",strokeWidth:"1.4",strokeLinecap:"round",strokeLinejoin:"round"})}),"KOD"]})]})})]}),e.jsx("div",{className:"note-content-area",children:u?e.jsxs("div",{className:"note-loading",children:[e.jsx("span",{className:"note-spinner"}),e.jsx("span",{children:"Ładowanie notatki..."})]}):l?e.jsx(_,{note:l}):e.jsxs("div",{className:"note-content-empty",children:[e.jsx("div",{className:"nce-icon",children:e.jsxs("svg",{width:"40",height:"40",viewBox:"0 0 40 40",fill:"none",children:[e.jsx("circle",{cx:"20",cy:"20",r:"17",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M13 20h14M20 13v14",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]})}),e.jsx("p",{className:"nce-title",children:"Brak notatki"}),e.jsxs("p",{className:"nce-sub",children:["Treść notatki pojawi się tutaj po wygenerowaniu przez AI.",e.jsx("br",{}),"Integracja z AI w przygotowaniu."]}),e.jsxs("div",{className:"nce-placeholder",children:[e.jsx("span",{className:"nce-placeholder-line",style:{width:"90%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"75%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"82%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"60%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"88%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"70%"}})]})]})}),e.jsxs("div",{className:"note-actions",children:[g&&e.jsxs("button",{className:"act-btn act-btn--code",title:"Wygeneruj zadania programistyczne",disabled:!0,children:[e.jsx("svg",{width:"15",height:"15",viewBox:"0 0 15 15",fill:"none",children:e.jsx("path",{d:"M10 3l3.5 4.5L10 12M5 3L1.5 7.5 5 12",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),"Wygeneruj zadania"]}),e.jsxs("div",{className:"act-right",children:[e.jsxs("button",{className:"act-btn act-btn--ask",onClick:()=>v(!0),children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("circle",{cx:"7",cy:"7",r:"6",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M5.5 5.5C5.5 4.67 6.17 4 7 4s1.5.67 1.5 1.5c0 .8-.8 1.3-1.5 1.5v1",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"}),e.jsx("circle",{cx:"7",cy:"10.5",r:"0.6",fill:"currentColor"})]}),"Dopytaj o..."]}),e.jsxs("button",{className:"act-btn act-btn--quiz",disabled:!0,title:"Wkrótce dostępne",children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("path",{d:"M2 2h10v10H2z",stroke:"currentColor",strokeWidth:"1.4",strokeLinejoin:"round"}),e.jsx("path",{d:"M5 5h4M5 7h4M5 9h2",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"})]}),"Rozpocznij quiz"]})]})]}),f&&e.jsx(F,{topicTitle:a.title,onClose:()=>v(!1)}),e.jsx(M,{})]})):e.jsxs("div",{className:"note-empty-screen",children:[e.jsxs("div",{className:"note-empty-inner",children:[e.jsxs("svg",{width:"48",height:"48",viewBox:"0 0 48 48",fill:"none",children:[e.jsx("rect",{x:"6",y:"4",width:"36",height:"40",rx:"4",stroke:"var(--border-1)",strokeWidth:"2"}),e.jsx("path",{d:"M14 16h20M14 22h20M14 28h14",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]}),e.jsx("h2",{className:"note-empty-title",children:"Wybierz temat"}),e.jsxs("p",{className:"note-empty-sub",children:["Kliknij temat z drzewa kategorii po lewej,",e.jsx("br",{}),"aby zobaczyć lub stworzyć notatkę."]})]}),e.jsx(M,{})]})}function _({note:a}){const r=a.content;return r?e.jsxs("article",{className:"note-article",children:[r.title&&e.jsx("h2",{className:"na-title",children:r.title}),r.summary&&e.jsx("p",{className:"na-summary",children:r.summary}),r.simpleExplanation&&e.jsxs("div",{className:"na-simple",children:[e.jsx("span",{className:"na-simple-label",children:"Jak dla dziecka"}),e.jsx("p",{children:r.simpleExplanation})]}),(r.sections??[]).map((o,l)=>e.jsxs("section",{className:"na-section",children:[e.jsx("h3",{className:"na-section-h",children:o.heading}),e.jsx("p",{className:"na-section-p",children:o.content})]},l)),(r.examples??[]).length>0&&e.jsxs("div",{className:"na-examples",children:[e.jsx("h3",{className:"na-sub-h",children:"Przykłady"}),r.examples.map((o,l)=>e.jsxs("div",{className:"na-example",children:[o.title&&e.jsx("p",{className:"na-example-title",children:o.title}),o.code&&e.jsx("pre",{className:"na-code",children:e.jsx("code",{children:o.code})}),o.explanation&&e.jsx("p",{className:"na-example-exp",children:o.explanation})]},l))]}),(r.memoryPoints??[]).length>0&&e.jsxs("div",{className:"na-list-section",children:[e.jsx("h3",{className:"na-sub-h",children:"Do zapamiętania"}),e.jsx("ul",{className:"na-list na-list--green",children:r.memoryPoints.map((o,l)=>e.jsx("li",{children:o},l))})]}),(r.commonMistakes??[]).length>0&&e.jsxs("div",{className:"na-list-section",children:[e.jsx("h3",{className:"na-sub-h",children:"Częste błędy"}),e.jsx("ul",{className:"na-list na-list--red",children:r.commonMistakes.map((o,l)=>e.jsx("li",{children:o},l))})]})]}):null}const Y={NEW:"Nowy",NOTE_GENERATED:"Notatka gotowa",QUIZ_READY:"Quiz dostępny",PASSED:"Zaliczony",MASTERED:"Opanowany"};function M(){return e.jsx("style",{children:`
      /* ── Pusty ── */
      .note-empty-screen {
        flex:1; display:flex; align-items:center; justify-content:center;
        background:var(--bg-0);
      }
      .note-empty-inner { text-align:center; }
      .note-empty-inner svg { margin:0 auto 16px; }
      .note-empty-title { font-size:1.1rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .note-empty-sub   { font-size:0.82rem; color:var(--text-3); line-height:1.7; }

      /* ── Panel główny ── */
      .note-panel {
        flex:1; display:flex; flex-direction:column; overflow:hidden;
        background:#fff;
      }

      /* Nagłówek */
      .note-header {
        padding:20px 28px 16px; border-bottom:1px solid var(--border-1); flex-shrink:0;
      }
      .note-breadcrumb { display:flex; align-items:center; gap:4px; margin-bottom:8px; }
      .note-bc-sep  { color:var(--text-3); font-size:0.75rem; margin:0 2px; }
      .note-bc-item { font-size:0.72rem; color:var(--text-3); }
      .note-title   { font-size:1.4rem; font-weight:800; color:var(--text-0); line-height:1.25; margin-bottom:10px; letter-spacing:-0.01em; }
      .note-meta    { display:flex; gap:8px; }
      .note-status-badge {
        font-family:var(--font-mono); font-size:0.62rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:3px 9px; border-radius:100px;
        border:1px solid var(--border-1); color:var(--text-3); background:var(--bg-2);
      }
      [data-status="NOTE_GENERATED"] { background:#eff6ff; border-color:rgba(37,99,235,.2); color:var(--accent); }
      [data-status="PASSED"]  { background:#f0fdf4; border-color:rgba(22,163,74,.25); color:#16a34a; }
      [data-status="MASTERED"]{ background:#fdf4ff; border-color:rgba(147,51,234,.25); color:#9333ea; }

      /* Konfiguracja */
      .note-config {
        padding:14px 28px; border-bottom:1px solid var(--border-1); flex-shrink:0;
        display:flex; flex-direction:column; gap:12px; background:var(--bg-2);
      }
      .note-config-row { display:flex; align-items:center; gap:16px; }
      .note-config-label { font-size:0.72rem; font-weight:700; color:var(--text-2); letter-spacing:0.04em; white-space:nowrap; min-width:130px; }

      /* Difficulty */
      .diff-group { display:flex; gap:6px; }
      .diff-btn {
        padding:6px 14px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:#fff;
        font-size:0.78rem; font-weight:600; color:var(--text-2);
        cursor:pointer; transition:all var(--t-fast); white-space:nowrap;
        display:flex; align-items:center; gap:5px;
      }
      .diff-btn:hover { border-color:var(--border-0); background:var(--bg-2); }
      .diff-btn--on   { font-weight:700; }
      .diff-btn:disabled { opacity:.6; cursor:wait; }
      .diff-saving {
        width:10px; height:10px; border-radius:50%;
        border:1.5px solid transparent; border-top-color:currentColor;
        animation:spin .6s linear infinite;
      }

      /* Checkbox kod */
      .code-check { display:flex; align-items:center; gap:10px; cursor:pointer; }
      .code-check-input { display:none; }
      .code-check-box {
        width:18px; height:18px; border-radius:5px; flex-shrink:0;
        border:1.5px solid var(--border-1); background:#fff;
        display:flex; align-items:center; justify-content:center;
        transition:all var(--t-fast);
      }
      .code-check-input:checked + .code-check-box {
        background:var(--accent); border-color:var(--accent);
      }
      .code-check-label { display:flex; flex-direction:column; gap:1px; }
      .code-check-text  { font-size:0.82rem; font-weight:600; color:var(--text-1); }
      .code-check-sub   { font-size:0.7rem; color:var(--text-3); }
      .code-check-badge {
        display:inline-flex; align-items:center; gap:4px;
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.1em; padding:3px 7px; border-radius:100px;
        background:#eff6ff; border:1px solid rgba(37,99,235,.2); color:var(--accent);
      }

      /* Treść */
      .note-content-area {
        flex:1; overflow-y:auto; padding:20px 28px;
      }
      .note-loading {
        display:flex; align-items:center; gap:10px;
        font-size:0.82rem; color:var(--text-2); padding:40px 0;
      }
      .note-spinner {
        width:16px; height:16px; border-radius:50%;
        border:2px solid var(--border-1); border-top-color:var(--accent);
        animation:spin .7s linear infinite;
      }

      /* Empty note placeholder */
      .note-content-empty {
        display:flex; flex-direction:column; align-items:center;
        padding:32px 0; gap:8px;
      }
      .nce-icon { color:var(--text-3); margin-bottom:4px; }
      .nce-title { font-size:0.95rem; font-weight:700; color:var(--text-2); }
      .nce-sub   { font-size:0.78rem; color:var(--text-3); text-align:center; line-height:1.7; }
      .nce-placeholder {
        width:100%; max-width:440px; margin-top:20px;
        display:flex; flex-direction:column; gap:10px;
      }
      .nce-placeholder-line {
        height:12px; border-radius:6px; display:block;
        background:linear-gradient(90deg, var(--bg-3) 25%, var(--bg-4) 50%, var(--bg-3) 75%);
        background-size:200% 100%;
        animation:shimmer 2s ease infinite;
      }

      /* Artikel notatki */
      .note-article { max-width:700px; }
      .na-title   { font-size:1.2rem; font-weight:800; color:var(--text-0); margin-bottom:10px; }
      .na-summary { font-size:0.9rem; color:var(--text-1); line-height:1.7; margin-bottom:16px; padding-bottom:16px; border-bottom:1px solid var(--border-1); }
      .na-simple  { background:var(--accent-light); border:1px solid var(--border-0); border-radius:var(--radius-md); padding:12px 16px; margin-bottom:20px; }
      .na-simple-label { font-size:0.68rem; font-weight:700; text-transform:uppercase; letter-spacing:.07em; color:var(--accent); display:block; margin-bottom:6px; }
      .na-simple p { font-size:0.85rem; color:var(--text-1); line-height:1.6; }
      .na-section { margin-bottom:18px; }
      .na-section-h { font-size:0.95rem; font-weight:700; color:var(--text-0); margin-bottom:6px; }
      .na-section-p { font-size:0.85rem; color:var(--text-1); line-height:1.7; }
      .na-sub-h { font-size:0.88rem; font-weight:700; color:var(--text-0); margin:20px 0 10px; }
      .na-example { background:var(--bg-2); border-radius:var(--radius-md); padding:12px 14px; margin-bottom:10px; }
      .na-example-title { font-size:0.8rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .na-code { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.8rem; color:var(--text-0); margin:8px 0; }
      .na-example-exp { font-size:0.8rem; color:var(--text-2); margin-top:6px; line-height:1.6; }
      .na-list-section { margin-bottom:12px; }
      .na-list { padding-left:18px; display:flex; flex-direction:column; gap:5px; }
      .na-list li { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-list--green li::marker { color:#16a34a; }
      .na-list--red   li::marker { color:var(--red); }

      /* Pasek akcji */
      .note-actions {
        padding:12px 20px; border-top:1px solid var(--border-1); flex-shrink:0;
        display:flex; align-items:center; gap:8px; background:#fff;
      }
      .act-right { margin-left:auto; display:flex; gap:8px; }
      .act-btn {
        display:inline-flex; align-items:center; gap:7px;
        padding:9px 16px; border-radius:var(--radius-md); border:1.5px solid var(--border-1);
        font-size:0.8rem; font-weight:600; cursor:pointer;
        transition:all var(--t-fast); white-space:nowrap;
      }
      .act-btn:disabled { opacity:.45; cursor:not-allowed; }
      .act-btn--code  { background:#f0fdf4; color:#16a34a; border-color:rgba(22,163,74,.3); }
      .act-btn--code:hover:not(:disabled)  { background:#dcfce7; }
      .act-btn--ask   { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
      .act-btn--ask:hover   { background:var(--accent-glow-strong); }
      .act-btn--quiz  { background:#f5f3ff; color:#7c3aed; border-color:rgba(124,58,237,.25); }
      .act-btn--quiz:hover:not(:disabled)  { background:#ede9fe; }
    `})}function q({parentId:a,parentName:r,onClose:o,onCreated:l}){const[c,u]=i.useState(""),[x,m]=i.useState(""),[h,g]=i.useState(!1),[j,p]=i.useState(""),b=async f=>{if(f.preventDefault(),!c.trim()){p("Nazwa jest wymagana");return}g(!0),p("");try{const v=await T.create({name:c.trim(),description:x.trim()||null,parentId:a??null});N.success(`Kategoria "${v.data.name}" utworzona`),l(v.data),o()}catch(v){p(v.message??"Błąd tworzenia kategorii")}finally{g(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:o,children:[e.jsxs("div",{className:"modal-box",onClick:f=>f.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:a?"Podkategoria":"Kategoria główna"}),e.jsx("h2",{className:"modal-title",children:a?`w "${r}"`:"Nowa kategoria"})]}),e.jsx("button",{className:"modal-close",onClick:o,children:"✕"})]}),e.jsxs("form",{onSubmit:b,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Nazwa *"}),e.jsx("input",{className:"mf-input",value:c,onChange:f=>{u(f.target.value),p("")},placeholder:a?"np. JAVA, SIECI, RENESANS":"np. IT, HISTORIA, MEDYCYNA",autoFocus:!0}),j&&e.jsx("span",{className:"mf-error",children:j})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Opis (opcjonalny)"}),e.jsx("input",{className:"mf-input",value:x,onChange:f=>m(f.target.value),placeholder:"Krótki opis kategorii"})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:o,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:h,children:h?e.jsx("span",{className:"btn-spin"}):"Utwórz"})]})]})]}),e.jsx(J,{})]})}function J(){return e.jsx("style",{children:`
      .modal-eyebrow { font-family:var(--font-mono); font-size:0.6rem; font-weight:700; letter-spacing:0.12em; text-transform:uppercase; color:var(--accent); margin-bottom:2px; }
      .modal-backdrop {
        position:fixed; inset:0; z-index:200;
        background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
        display:flex; align-items:center; justify-content:center; padding:16px;
      }
      .modal-box {
        background:#fff; border-radius:var(--radius-xl);
        border:1px solid var(--border-1);
        box-shadow:var(--shadow-lg);
        width:100%; max-width:420px;
        animation:fadeIn .2s ease both;
      }
      .modal-head {
        display:flex; align-items:center; justify-content:space-between;
        padding:18px 20px 0;
      }
      .modal-title { font-size:1rem; font-weight:700; color:var(--text-0); }
      .modal-close {
        background:none; border:none; color:var(--text-3);
        cursor:pointer; font-size:0.9rem; padding:4px 6px;
        border-radius:6px; transition:background var(--t-fast);
      }
      .modal-close:hover { background:var(--bg-2); color:var(--text-1); }
      .modal-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
      .modal-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }
      .mf-field { display:flex; flex-direction:column; gap:5px; }
      .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
      .mf-input {
        padding:10px 12px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:var(--bg-2);
        font-size:0.88rem; color:var(--text-0); outline:none;
        transition:border-color .15s;
      }
      .mf-input:focus { border-color:var(--accent); background:#fff; box-shadow:0 0 0 3px var(--accent-glow); }
      .mf-error { font-size:0.72rem; color:var(--red); }
      .btn-primary {
        padding:9px 20px; background:var(--accent); color:#fff;
        border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700;
        cursor:pointer; transition:background .15s;
      }
      .btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
      .btn-primary:disabled { opacity:.5; cursor:not-allowed; }
      .btn-ghost {
        padding:9px 16px; background:transparent; color:var(--text-2);
        border:1px solid var(--border-1); border-radius:var(--radius-md);
        font-size:0.84rem; cursor:pointer; transition:background .15s;
      }
      .btn-ghost:hover { background:var(--bg-2); }
      .btn-spin {
        display:inline-block; width:13px; height:13px;
        border:2px solid rgba(255,255,255,.3); border-top-color:#fff;
        border-radius:50%; animation:spin .6s linear infinite; vertical-align:middle;
      }
    `})}const V=[{value:"BASIC",label:"Podstawowy",color:"#16a34a"},{value:"MEDIUM",label:"Średni",color:"#d97706"},{value:"HARD",label:"Zaawansowany",color:"#dc2626"}];function G({categoryId:a,categoryName:r,onClose:o,onCreated:l}){const[c,u]=i.useState(""),[x,m]=i.useState(""),[h,g]=i.useState("BASIC"),[j,p]=i.useState(!1),[b,f]=i.useState({}),v=()=>{const t={};return c.trim()||(t.title="Tytuł jest wymagany"),x.trim()||(t.shortPrompt="Kontekst jest wymagany"),t},y=async t=>{t.preventDefault();const s=v();if(Object.keys(s).length){f(s);return}p(!0);try{const w=await S.create({categoryId:a,title:c.trim(),shortPrompt:x.trim(),difficulty:h});N.success(`Temat "${w.data.title}" utworzony`),l(w.data),o()}catch(w){N.error(w.message??"Błąd tworzenia tematu")}finally{p(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:o,children:[e.jsxs("div",{className:"modal-box modal-box--md",onClick:t=>t.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("h2",{className:"modal-title",children:['Nowy temat w "',r,'"']}),e.jsx("button",{className:"modal-close",onClick:o,children:"✕"})]}),e.jsxs("form",{onSubmit:y,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Tytuł tematu *"}),e.jsx("input",{className:`mf-input${b.title?" mf-input--err":""}`,value:c,onChange:t=>{u(t.target.value),f(s=>({...s,title:""}))},placeholder:"np. Polimorfizm w Javie",autoFocus:!0}),b.title&&e.jsx("span",{className:"mf-error",children:b.title})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Kontekst dla AI *"}),e.jsx("textarea",{className:`mf-input mf-textarea${b.shortPrompt?" mf-input--err":""}`,value:x,onChange:t=>{m(t.target.value),f(s=>({...s,shortPrompt:""}))},placeholder:"Krótki opis co chcesz wiedzieć o tym temacie, np. wytłumacz z przykładami kodu, skupiony na praktycznym użyciu...",rows:3}),b.shortPrompt&&e.jsx("span",{className:"mf-error",children:b.shortPrompt})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:V.map(t=>e.jsxs("label",{className:`diff-opt${h===t.value?" diff-opt--on":""}`,style:h===t.value?{borderColor:t.color,background:t.color+"12",color:t.color}:{},children:[e.jsx("input",{type:"radio",name:"difficulty",value:t.value,checked:h===t.value,onChange:()=>g(t.value),style:{display:"none"}}),t.label]},t.value))})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:o,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:j,children:j?e.jsx("span",{className:"btn-spin"}):"Utwórz temat"})]})]})]}),e.jsx("style",{children:`
        .modal-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .modal-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:420px;
          animation:fadeIn .2s ease both;
        }
        .modal-box--md { max-width:480px; }
        .modal-head { display:flex; align-items:center; justify-content:space-between; padding:18px 20px 0; }
        .modal-title { font-size:1rem; font-weight:700; color:var(--text-0); }
        .modal-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; transition:background var(--t-fast); }
        .modal-close:hover { background:var(--bg-2); }
        .modal-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
        .modal-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }
        .mf-field { display:flex; flex-direction:column; gap:5px; }
        .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
        .mf-input {
          padding:10px 12px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-2);
          font-size:0.88rem; color:var(--text-0); outline:none;
          transition:border-color .15s; resize:vertical;
        }
        .mf-input:focus { border-color:var(--accent); background:#fff; box-shadow:0 0 0 3px var(--accent-glow); }
        .mf-input--err { border-color:var(--red) !important; }
        .mf-textarea { font-family:inherit; min-height:72px; }
        .mf-error { font-size:0.72rem; color:var(--red); }
        .diff-group { display:flex; gap:8px; }
        .diff-opt {
          flex:1; text-align:center; padding:8px 4px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          font-size:0.8rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all .15s; user-select:none;
        }
        .diff-opt:hover { border-color:var(--border-0); }
        .btn-primary { padding:9px 20px; background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700; cursor:pointer; transition:background .15s; }
        .btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
        .btn-primary:disabled { opacity:.5; cursor:not-allowed; }
        .btn-ghost { padding:9px 16px; background:transparent; color:var(--text-2); border:1px solid var(--border-1); border-radius:var(--radius-md); font-size:0.84rem; cursor:pointer; transition:background .15s; }
        .btn-ghost:hover { background:var(--bg-2); }
        .btn-spin { display:inline-block; width:13px; height:13px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .6s linear infinite; vertical-align:middle; }
      `})]})}function te(){W("Dashboard");const[a,r]=i.useState([]),[o,l]=i.useState(!0),[c,u]=i.useState(new Set),[x,m]=i.useState({}),[h,g]=i.useState(new Set),[j,p]=i.useState(null),[b,f]=i.useState([]),[v,y]=i.useState(null),[t,s]=i.useState(null),w=i.useCallback(async()=>{l(!0);try{const n=await T.getTree();r(n.data.children??[])}catch(n){n.status!==401&&N.error("Błąd ładowania kategorii")}finally{l(!1)}},[]);i.useEffect(()=>{w()},[w]);const A=i.useCallback(async n=>{if(u(d=>{const k=new Set(d);return k.has(n)?(k.delete(n),k):(k.add(n),k)}),!x[n]){g(d=>new Set(d).add(n));try{const d=await S.listByCategory(n);m(k=>({...k,[n]:d.data}))}catch{}finally{g(d=>{const k=new Set(d);return k.delete(n),k})}}},[x]),D=i.useCallback(n=>{p(n),f(C(a,n.categoryId))},[a]),L=i.useCallback(n=>{w(),n.parentId&&u(d=>new Set(d).add(n.parentId))},[w]),E=i.useCallback(n=>{m(d=>({...d,[n.categoryId]:[...d[n.categoryId]??[],n]})),u(d=>new Set(d).add(n.categoryId)),p(n),f(C(a,n.categoryId))},[a]),B=i.useCallback(n=>{m(d=>({...d,[n.categoryId]:(d[n.categoryId]??[]).map(k=>k.id===n.id?n:k)})),p(d=>d?.id===n.id?n:d)},[]);return e.jsxs("div",{className:"db-root",children:[e.jsx(H,{tree:a,loading:o,expanded:c,topics:x,loadingTopics:h,selectedTopicId:j?.id,onToggle:A,onSelectTopic:D,onAddRootCategory:()=>y({parentId:null,parentName:null}),onAddSubcategory:(n,d)=>y({parentId:n,parentName:d}),onAddTopic:(n,d)=>s({categoryId:n,categoryName:d})}),e.jsx(Z,{topic:j,categoryPath:b,onTopicUpdated:B}),v!==null&&e.jsx(q,{parentId:v.parentId,parentName:v.parentName,onClose:()=>y(null),onCreated:L}),t!==null&&e.jsx(G,{categoryId:t.categoryId,categoryName:t.categoryName,onClose:()=>s(null),onCreated:E}),e.jsx("style",{children:`
        .db-root {
          display: flex;
          height: calc(100vh - 56px); /* topbar height */
          margin: -28px -32px;        /* negate main-content padding */
          overflow: hidden;
        }
      `})]})}function C(a,r,o=[]){for(const l of a){const c=[...o,{id:l.id,name:l.name}];if(l.id===r)return c;if(l.children?.length){const u=C(l.children,r,c);if(u)return u}}return[]}export{te as default};
