import{j as e,r as n,t as C}from"./index-5ZkII2CN.js";import{u as R}from"./index-DscXlyxZ.js";import{n as O,t as T,c as A}from"./services-D6-2Q08e.js";const $=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M3.5 2L7 5.5 3.5 9",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),F=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M2 3.5L5.5 7 9 3.5",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),K=()=>e.jsxs("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1",width:"9",height:"10",rx:"1.5",stroke:"currentColor",strokeWidth:"1.3"}),e.jsx("path",{d:"M3.5 4h5M3.5 6.5h5M3.5 9h3",stroke:"currentColor",strokeWidth:"1.2",strokeLinecap:"round"})]}),S=()=>e.jsx("svg",{width:"10",height:"10",viewBox:"0 0 10 10",fill:"none",children:e.jsx("path",{d:"M5 1.5v7M1.5 5h7",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),H={BASIC:"#16a34a",MEDIUM:"#d97706",HARD:"#dc2626"};function D({node:t,depth:a=0,expanded:r,topics:s,loadingTopics:p,selectedTopicId:m,onToggle:u,onSelectTopic:k,onAddSubcategory:g,onAddTopic:v}){const[f,h]=n.useState(!1),y=r.has(t.id),b=s[t.id]??[],x=p.has(t.id),j=a===0,i=a===1;return e.jsxs("div",{className:`tn-block tn-block--d${a}`,children:[e.jsxs("div",{className:`tn-row tn-row--d${a}${y?" tn-row--open":""}`,onMouseEnter:()=>h(!0),onMouseLeave:()=>h(!1),children:[e.jsx("button",{className:"tn-chevron",onClick:()=>u(t.id),children:y?e.jsx(F,{}):e.jsx($,{})}),e.jsx("span",{className:"tn-name",onClick:()=>u(t.id),title:t.name,children:t.name}),f&&e.jsxs("div",{className:"tn-actions",children:[j&&e.jsxs("button",{className:"tn-act tn-act--sub",title:"Dodaj podkategorię",onClick:l=>{l.stopPropagation(),g(t.id,t.name)},children:[e.jsx(S,{})," ",e.jsx("span",{children:"Podkat."})]}),i&&e.jsxs("button",{className:"tn-act tn-act--topic",title:"Dodaj temat / notatkę",onClick:l=>{l.stopPropagation(),v(t.id,t.name)},children:[e.jsx(S,{})," ",e.jsx("span",{children:"Temat"})]}),!j&&!i&&e.jsxs(e.Fragment,{children:[e.jsxs("button",{className:"tn-act tn-act--sub",onClick:l=>{l.stopPropagation(),g(t.id,t.name)},children:[e.jsx(S,{})," ",e.jsx("span",{children:"Podkat."})]}),e.jsxs("button",{className:"tn-act tn-act--topic",onClick:l=>{l.stopPropagation(),v(t.id,t.name)},children:[e.jsx(S,{})," ",e.jsx("span",{children:"Temat"})]})]})]})]}),y&&e.jsxs("div",{className:"tn-children",children:[(t.children??[]).map(l=>e.jsx(D,{node:l,depth:a+1,expanded:r,topics:s,loadingTopics:p,selectedTopicId:m,onToggle:u,onSelectTopic:k,onAddSubcategory:g,onAddTopic:v},l.id)),x&&e.jsxs("div",{className:"tn-loading",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}),!x&&b.map(l=>e.jsxs("button",{className:`tn-topic${m===l.id?" tn-topic--active":""}`,onClick:()=>k(l),children:[e.jsx("span",{className:"tn-topic-icon",children:e.jsx(K,{})}),e.jsx("span",{className:"tn-topic-name",title:l.title,children:l.title}),e.jsx("span",{className:"tn-topic-diff",title:l.difficulty,style:{background:H[l.difficulty]}})]},l.id)),!x&&b.length===0&&(t.children??[]).length===0&&e.jsx("p",{className:"tn-empty-hint",children:j?"Brak podkategorii":"Brak tematów"})]})]})}function U({tree:t,expanded:a,topics:r,loadingTopics:s,selectedTopicId:p,onToggle:m,onSelectTopic:u,onAddRootCategory:k,onAddSubcategory:g,onAddTopic:v,loading:f}){return e.jsxs("aside",{className:"tree-panel",children:[e.jsxs("div",{className:"tree-head",children:[e.jsx("span",{className:"tree-head-title",children:"Baza wiedzy"}),e.jsx("button",{className:"tree-add-btn",onClick:k,title:"Nowa kategoria (np. IT, HISTORIA)",children:e.jsx(S,{})})]}),e.jsxs("div",{className:"tree-legend",children:[e.jsx("span",{className:"tree-legend-item tree-legend-item--cat",children:"Kategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--sub",children:"Podkategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--topic",children:"Temat"})]}),e.jsx("div",{className:"tree-body",children:f?e.jsxs("div",{className:"tree-status",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}):t.length===0?e.jsxs("div",{className:"tree-empty",children:[e.jsx("svg",{width:"36",height:"36",viewBox:"0 0 36 36",fill:"none",style:{color:"var(--border-1)"},children:e.jsx("path",{d:"M4 10a2 2 0 012-2h8l2 2h14a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V10z",stroke:"currentColor",strokeWidth:"1.5"})}),e.jsx("p",{className:"tree-empty-title",children:"Brak kategorii"}),e.jsxs("p",{className:"tree-empty-sub",children:["Zacznij od dodania kategorii",e.jsx("br",{}),"np. ",e.jsx("em",{children:"IT"})," lub ",e.jsx("em",{children:"HISTORIA"})]}),e.jsx("button",{className:"tree-empty-btn",onClick:k,children:"Dodaj kategorię →"})]}):t.map(h=>e.jsx(D,{node:h,depth:0,expanded:a,topics:r,loadingTopics:s,selectedTopicId:p,onToggle:m,onSelectTopic:u,onAddSubcategory:g,onAddTopic:v},h.id))}),e.jsx("style",{children:`
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
      `})]})}function Z({topicTitle:t,onClose:a}){return e.jsxs("div",{className:"modal-backdrop",onClick:a,children:[e.jsxs("div",{className:"modal-box",onClick:r=>r.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:"AI · Dopytaj o temat"}),e.jsx("h2",{className:"modal-title",children:t})]}),e.jsx("button",{className:"modal-close",onClick:a,children:"✕"})]}),e.jsxs("div",{className:"modal-body",children:[e.jsx("p",{className:"modal-info",children:"Zadaj dodatkowe pytanie dotyczące tej notatki. AI uzupełni lub rozszerzy treść na podstawie Twojego pytania."}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Twoje pytanie"}),e.jsx("textarea",{className:"mf-input mf-textarea",placeholder:"np. Wyjaśnij prościej, dodaj przykład z życia codziennego, pokaż jak to działa w Spring Boot...",rows:4,disabled:!0})]}),e.jsxs("div",{className:"modal-placeholder",children:[e.jsx("span",{className:"modal-placeholder-icon",children:"🔧"}),e.jsx("span",{children:"Integracja AI w przygotowaniu"})]})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{className:"btn-ghost",onClick:a,children:"Zamknij"}),e.jsx("button",{className:"btn-primary",disabled:!0,title:"Wkrótce dostępne",children:"Wyślij pytanie →"})]})]}),e.jsx("style",{children:`
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
      `})]})}const _=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}],V={NEW:"Nowy",NOTE_GENERATED:"Notatka gotowa",QUIZ_READY:"Quiz dostępny",PASSED:"Zaliczony",MASTERED:"Opanowany"};function Y({topic:t,categoryPath:a,onTopicUpdated:r,onOpenTree:s}){const[p,m]=n.useState(null),[u,k]=n.useState(!1),[g,v]=n.useState(t?.difficulty??"BASIC"),[f,h]=n.useState(!1),[y,b]=n.useState(!1);n.useEffect(()=>{if(!t){m(null);return}v(t.difficulty),k(!0),O.getByTopic(t.id).then(i=>m(i.data)).catch(i=>{i.status===404?m(null):C.error("Błąd ładowania notatki")}).finally(()=>k(!1))},[t?.id]);const x=async i=>{if(!(i===g||f)){v(i),h(!0);try{const l=await T.update(t.id,{difficulty:i});r?.(l.data)}catch{C.error("Błąd zapisu trudności"),v(t.difficulty)}finally{h(!1)}}};if(!t)return e.jsxs("div",{className:"np-empty",children:[e.jsxs("button",{className:"np-tree-toggle",onClick:s,title:"Otwórz drzewo kategorii",children:[e.jsx("svg",{width:"18",height:"18",viewBox:"0 0 18 18",fill:"none",children:e.jsx("path",{d:"M3 5h12M3 9h8M3 13h10",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),"Kategorie"]}),e.jsxs("div",{className:"np-empty-inner",children:[e.jsxs("svg",{width:"52",height:"52",viewBox:"0 0 52 52",fill:"none",children:[e.jsx("rect",{x:"8",y:"6",width:"36",height:"40",rx:"4",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M16 18h20M16 25h20M16 32h12",stroke:"var(--border-1)",strokeWidth:"1.8",strokeLinecap:"round"})]}),e.jsx("h2",{className:"np-empty-title",children:"Wybierz temat"}),e.jsxs("p",{className:"np-empty-sub",children:["Kliknij temat z drzewa kategorii,",e.jsx("br",{}),"aby wyświetlić lub stworzyć notatkę."]})]}),e.jsx(I,{})]});const j=t.code??!1;return e.jsxs("div",{className:"note-panel",children:[e.jsxs("div",{className:"note-header",children:[e.jsxs("div",{className:"note-header-top",children:[e.jsx("button",{className:"np-tree-toggle np-tree-toggle--inline",onClick:s,title:"Otwórz drzewo",children:e.jsx("svg",{width:"16",height:"16",viewBox:"0 0 16 16",fill:"none",children:e.jsx("path",{d:"M2 4.5h12M2 8.5h8M2 12.5h10",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round"})})}),e.jsx("div",{className:"note-breadcrumb",children:(a??[]).map((i,l)=>e.jsxs("span",{children:[l>0&&e.jsx("span",{className:"bc-sep",children:"›"}),e.jsx("span",{className:"bc-item",children:i.name})]},i.id))}),e.jsxs("div",{className:"note-badges",children:[e.jsx("span",{className:"note-status","data-status":t.status,children:V[t.status]??t.status}),j&&e.jsxs("span",{className:"note-code-badge",children:[e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M7.5 2l3 3.5-3 3.5M3.5 2l-3 3.5 3 3.5",stroke:"currentColor",strokeWidth:"1.4",strokeLinecap:"round",strokeLinejoin:"round"})}),"KOD"]})]})]}),e.jsx("h1",{className:"note-title",children:t.title})]}),e.jsx("div",{className:"note-config",children:e.jsxs("div",{className:"note-config-row",children:[e.jsx("span",{className:"note-config-label",children:"Trudność"}),e.jsx("div",{className:"diff-group",children:_.map(i=>e.jsxs("button",{className:`diff-btn${g===i.value?" diff-btn--on":""}`,style:g===i.value?{background:i.bg,borderColor:i.border,color:i.color}:{},onClick:()=>x(i.value),disabled:f,children:[i.label,g===i.value&&f&&e.jsx("span",{className:"diff-saving"})]},i.value))})]})}),e.jsx("div",{className:"note-content-area",children:u?e.jsxs("div",{className:"note-loading",children:[e.jsx("span",{className:"note-spinner"})," Ładowanie notatki..."]}):p?e.jsx(J,{note:p}):e.jsx(q,{})}),e.jsxs("div",{className:"note-actions",children:[j&&e.jsxs("button",{className:"act-btn act-btn--code",disabled:!0,title:"Wkrótce — wymaga AI",children:[e.jsx("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:e.jsx("path",{d:"M9.5 2.5l3 4-3 4M4.5 2.5l-3 4 3 4",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round",strokeLinejoin:"round"})}),"Wygeneruj zadania"]}),e.jsx("div",{className:"act-spacer"}),e.jsxs("button",{className:"act-btn act-btn--ask",onClick:()=>b(!0),children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("circle",{cx:"7",cy:"7",r:"6",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M5.3 5.5C5.5 4.6 6.1 4 7 4c.9 0 1.7.7 1.7 1.5 0 .9-.9 1.4-1.7 1.6v.9",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"}),e.jsx("circle",{cx:"7",cy:"10.5",r:"0.6",fill:"currentColor"})]}),"Dopytaj o..."]}),e.jsxs("button",{className:"act-btn act-btn--quiz",disabled:!0,title:"Wkrótce dostępne",children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1.5",width:"11",height:"11",rx:"2",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M4 4.5h6M4 7h6M4 9.5h4",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"})]}),"Rozpocznij quiz"]})]}),y&&e.jsx(Z,{topicTitle:t.title,onClose:()=>b(!1)}),e.jsx(I,{})]})}function q(){return e.jsxs("div",{className:"note-empty-content",children:[e.jsxs("div",{className:"nec-top",children:[e.jsxs("svg",{width:"44",height:"44",viewBox:"0 0 44 44",fill:"none",children:[e.jsx("circle",{cx:"22",cy:"22",r:"19",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M14 22h16M22 14v16",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]}),e.jsx("p",{className:"nec-title",children:"Brak notatki"}),e.jsxs("p",{className:"nec-sub",children:["Treść zostanie wygenerowana przez AI.",e.jsx("br",{}),"Integracja w przygotowaniu."]})]}),e.jsx("div",{className:"nec-skeleton",children:[90,75,83,60,88,70,55,78].map((t,a)=>e.jsx("span",{className:"nec-line",style:{width:`${t}%`,animationDelay:`${a*.12}s`}},a))})]})}function J({note:t}){const a=t.content;return a?e.jsxs("article",{className:"note-article",children:[a.title&&e.jsx("h2",{className:"na-title",children:a.title}),a.summary&&e.jsx("p",{className:"na-summary",children:a.summary}),a.simpleExplanation&&e.jsxs("div",{className:"na-simple",children:[e.jsx("span",{className:"na-simple-label",children:"Jak dla dziecka"}),e.jsx("p",{children:a.simpleExplanation})]}),(a.sections??[]).map((r,s)=>e.jsxs("section",{className:"na-section",children:[e.jsx("h3",{className:"na-section-h",children:r.heading}),e.jsx("p",{className:"na-section-p",children:r.content})]},s)),(a.examples??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Przykłady"}),a.examples.map((r,s)=>e.jsxs("div",{className:"na-example",children:[r.title&&e.jsx("p",{className:"na-example-title",children:r.title}),r.code&&e.jsx("pre",{className:"na-code",children:e.jsx("code",{children:r.code})}),r.explanation&&e.jsx("p",{className:"na-example-exp",children:r.explanation})]},s))]}),(a.memoryPoints??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Do zapamiętania"}),e.jsx("ul",{className:"na-list na-list--green",children:a.memoryPoints.map((r,s)=>e.jsx("li",{children:r},s))})]}),(a.commonMistakes??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Częste błędy"}),e.jsx("ul",{className:"na-list na-list--red",children:a.commonMistakes.map((r,s)=>e.jsx("li",{children:r},s))})]})]}):null}function I(){return e.jsx("style",{children:`
      /* ── Puste ── */
      .np-empty {
        flex:1; display:flex; flex-direction:column;
        align-items:center; justify-content:center;
        background:var(--bg-0); position:relative; padding:20px;
      }
      .np-empty-inner { text-align:center; }
      .np-empty-inner svg { margin:0 auto 16px; }
      .np-empty-title { font-size:1.1rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .np-empty-sub   { font-size:0.82rem; color:var(--text-3); line-height:1.7; }

      /* Hamburger przycisk (tablet/mobile) */
      .np-tree-toggle {
        display:none;
        position:absolute; top:16px; left:16px;
        align-items:center; gap:6px;
        padding:7px 12px; background:#fff;
        border:1px solid var(--border-1); border-radius:var(--radius-md);
        font-size:0.78rem; font-weight:600; color:var(--text-1);
        cursor:pointer; box-shadow:var(--shadow-sm);
        transition:background var(--t-fast);
      }
      .np-tree-toggle:hover { background:var(--bg-2); }
      .np-tree-toggle--inline {
        position:static; flex-shrink:0;
      }

      /* ── Panel ── */
      .note-panel {
        flex:1; display:flex; flex-direction:column;
        overflow:hidden; background:#fff;
        min-width:0;
      }

      /* Header */
      .note-header { padding:18px 24px 14px; border-bottom:1px solid var(--border-1); flex-shrink:0; }
      .note-header-top {
        display:flex; align-items:center; gap:8px; margin-bottom:8px; flex-wrap:wrap;
      }
      .note-breadcrumb {
        flex:1; display:flex; align-items:center; gap:3px; min-width:0; flex-wrap:wrap;
      }
      .bc-sep  { color:var(--text-3); font-size:0.72rem; margin:0 2px; }
      .bc-item { font-size:0.71rem; color:var(--text-3); white-space:nowrap; }
      .note-badges { display:flex; gap:5px; align-items:center; flex-shrink:0; }
      .note-status {
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:3px 8px; border-radius:100px;
        background:var(--bg-2); border:1px solid var(--border-1); color:var(--text-3);
      }
      [data-status="NOTE_GENERATED"] { background:#eff6ff; border-color:rgba(37,99,235,.2); color:var(--accent); }
      [data-status="PASSED"]         { background:#f0fdf4; border-color:rgba(22,163,74,.25); color:#16a34a; }
      [data-status="MASTERED"]       { background:#fdf4ff; border-color:rgba(147,51,234,.25); color:#9333ea; }
      .note-code-badge {
        display:inline-flex; align-items:center; gap:4px;
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.1em; padding:3px 8px; border-radius:100px;
        background:#eff6ff; border:1px solid rgba(37,99,235,.2); color:var(--accent);
      }
      .note-title { font-size:1.3rem; font-weight:800; color:var(--text-0); line-height:1.25; letter-spacing:-0.01em; }

      /* Config */
      .note-config {
        padding:12px 24px; border-bottom:1px solid var(--border-1);
        flex-shrink:0; background:var(--bg-2);
      }
      .note-config-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
      .note-config-label { font-size:0.71rem; font-weight:700; color:var(--text-2); letter-spacing:0.04em; white-space:nowrap; }
      .diff-group { display:flex; gap:5px; flex-wrap:wrap; }
      .diff-btn {
        padding:5px 12px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:#fff;
        font-size:0.77rem; font-weight:600; color:var(--text-2);
        cursor:pointer; transition:all var(--t-fast); display:flex; align-items:center; gap:4px;
      }
      .diff-btn:hover { border-color:var(--border-0); }
      .diff-btn--on   { font-weight:700; }
      .diff-btn:disabled { opacity:.6; cursor:wait; }
      .diff-saving { width:9px; height:9px; border-radius:50%; border:1.5px solid transparent; border-top-color:currentColor; animation:spin .6s linear infinite; }

      /* Content */
      .note-content-area { flex:1; overflow-y:auto; padding:20px 24px; }
      .note-loading { display:flex; align-items:center; gap:10px; font-size:0.82rem; color:var(--text-2); padding:40px 0; }
      .note-spinner { width:16px; height:16px; border-radius:50%; border:2px solid var(--border-1); border-top-color:var(--accent); animation:spin .7s linear infinite; }

      /* Empty content */
      .note-empty-content { display:flex; flex-direction:column; align-items:center; gap:20px; padding:24px 0; }
      .nec-top { text-align:center; }
      .nec-top svg { margin:0 auto 12px; }
      .nec-title { font-size:0.95rem; font-weight:700; color:var(--text-2); margin-bottom:5px; }
      .nec-sub   { font-size:0.78rem; color:var(--text-3); line-height:1.6; }
      .nec-skeleton { width:100%; max-width:500px; display:flex; flex-direction:column; gap:10px; }
      .nec-line {
        height:11px; border-radius:6px; display:block;
        background:linear-gradient(90deg, var(--bg-3) 25%, var(--bg-4) 50%, var(--bg-3) 75%);
        background-size:200% 100%; animation:shimmer 2s ease infinite;
      }

      /* Artikel */
      .note-article { max-width:680px; }
      .na-title   { font-size:1.15rem; font-weight:800; color:var(--text-0); margin-bottom:10px; }
      .na-summary { font-size:0.88rem; color:var(--text-1); line-height:1.7; margin-bottom:16px; padding-bottom:16px; border-bottom:1px solid var(--border-1); }
      .na-simple  { background:var(--accent-light); border:1px solid var(--border-0); border-radius:var(--radius-md); padding:12px 16px; margin-bottom:20px; }
      .na-simple-label { font-size:0.65rem; font-weight:700; text-transform:uppercase; letter-spacing:.08em; color:var(--accent); display:block; margin-bottom:5px; }
      .na-simple p { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-section { margin-bottom:16px; }
      .na-section-h { font-size:0.93rem; font-weight:700; color:var(--text-0); margin-bottom:5px; }
      .na-section-p { font-size:0.84rem; color:var(--text-1); line-height:1.7; }
      .na-sub-h { font-size:0.88rem; font-weight:700; color:var(--text-0); margin:18px 0 10px; }
      .na-example { background:var(--bg-2); border-radius:var(--radius-md); padding:12px 14px; margin-bottom:10px; }
      .na-example-title { font-size:0.79rem; font-weight:700; color:var(--text-1); margin-bottom:7px; }
      .na-code { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.79rem; color:var(--text-0); margin:7px 0; }
      .na-example-exp { font-size:0.79rem; color:var(--text-2); margin-top:5px; line-height:1.6; }
      .na-list { padding-left:18px; display:flex; flex-direction:column; gap:5px; }
      .na-list li { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-list--green li::marker { color:#16a34a; }
      .na-list--red   li::marker { color:var(--red); }

      /* Actions */
      .note-actions {
        padding:10px 16px; border-top:1px solid var(--border-1); flex-shrink:0;
        display:flex; align-items:center; gap:6px; background:#fff;
        flex-wrap:wrap;
      }
      .act-spacer { flex:1; }
      .act-btn {
        display:inline-flex; align-items:center; gap:6px;
        padding:8px 14px; border-radius:var(--radius-md); border:1.5px solid var(--border-1);
        font-size:0.78rem; font-weight:600; cursor:pointer;
        transition:all var(--t-fast); white-space:nowrap; background:#fff;
      }
      .act-btn:disabled { opacity:.45; cursor:not-allowed; }
      .act-btn--code  { background:#f0fdf4; color:#16a34a; border-color:rgba(22,163,74,.3); }
      .act-btn--code:hover:not(:disabled)  { background:#dcfce7; }
      .act-btn--ask   { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
      .act-btn--ask:hover   { background:var(--accent-glow-strong); }
      .act-btn--quiz  { background:#f5f3ff; color:#7c3aed; border-color:rgba(124,58,237,.25); }
      .act-btn--quiz:hover:not(:disabled) { background:#ede9fe; }

      /* ── Responsywność ── */
      @media (max-width: 860px) {
        .np-tree-toggle { display:flex; }
        .np-tree-toggle--inline { display:flex; }
        .note-title { font-size:1.1rem; }
        .note-header { padding:14px 16px 12px; }
        .note-config { padding:10px 16px; }
        .note-content-area { padding:16px; }
        .note-actions { padding:10px 12px; }
        .act-btn { padding:8px 10px; font-size:0.74rem; }
        .diff-btn { padding:5px 8px; font-size:0.72rem; }
      }

      @media (max-width: 540px) {
        .act-btn span:not(.act-icon) { display:none; }
        .note-title { font-size:1rem; }
        .bc-item { font-size:0.68rem; }
      }
    `})}function X({parentId:t,parentName:a,onClose:r,onCreated:s}){const[p,m]=n.useState(""),[u,k]=n.useState(""),[g,v]=n.useState(!1),[f,h]=n.useState(""),y=async b=>{if(b.preventDefault(),!p.trim()){h("Nazwa jest wymagana");return}v(!0),h("");try{const x=await A.create({name:p.trim(),description:u.trim()||null,parentId:t??null});C.success(`Kategoria "${x.data.name}" utworzona`),s(x.data),r()}catch(x){h(x.message??"Błąd tworzenia kategorii")}finally{v(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:r,children:[e.jsxs("div",{className:"modal-box",onClick:b=>b.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:t?"Podkategoria":"Kategoria główna"}),e.jsx("h2",{className:"modal-title",children:t?`w "${a}"`:"Nowa kategoria"})]}),e.jsx("button",{className:"modal-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:y,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Nazwa *"}),e.jsx("input",{className:"mf-input",value:p,onChange:b=>{m(b.target.value),h("")},placeholder:t?"np. JAVA, SIECI, RENESANS":"np. IT, HISTORIA, MEDYCYNA",autoFocus:!0}),f&&e.jsx("span",{className:"mf-error",children:f})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Opis (opcjonalny)"}),e.jsx("input",{className:"mf-input",value:u,onChange:b=>k(b.target.value),placeholder:"Krótki opis kategorii"})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:g,children:g?e.jsx("span",{className:"btn-spin"}):"Utwórz"})]})]})]}),e.jsx(G,{})]})}function G(){return e.jsx("style",{children:`
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
    `})}const Q=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}];function ee({categoryId:t,categoryName:a,onClose:r,onCreated:s}){const[p,m]=n.useState(""),[u,k]=n.useState(""),[g,v]=n.useState("BASIC"),[f,h]=n.useState(!1),[y,b]=n.useState(!1),[x,j]=n.useState({}),i=()=>{const d={};return p.trim()||(d.title="Tytuł jest wymagany"),u.trim()||(d.shortPrompt="Kontekst jest wymagany"),d},l=async d=>{d.preventDefault();const N=i();if(Object.keys(N).length){j(N);return}b(!0);try{const z=await T.create({categoryId:t,title:p.trim(),shortPrompt:u.trim(),difficulty:g,code:f});C.success(`Temat "${z.data.title}" utworzony`),s(z.data),r()}catch(z){C.error(z.message??"Błąd tworzenia tematu")}finally{b(!1)}};return e.jsxs("div",{className:"tm-backdrop",onClick:r,children:[e.jsxs("div",{className:"tm-box",onClick:d=>d.stopPropagation(),children:[e.jsxs("header",{className:"tm-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"tm-eyebrow",children:"Nowy temat"}),e.jsxs("h2",{className:"tm-title",children:['w "',a,'"']})]}),e.jsx("button",{className:"tm-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:l,className:"tm-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Tytuł tematu *"}),e.jsx("input",{className:`mf-input${x.title?" mf-input--err":""}`,value:p,onChange:d=>{m(d.target.value),j(N=>({...N,title:""}))},placeholder:"np. Polimorfizm w Javie",autoFocus:!0}),x.title&&e.jsx("span",{className:"mf-error",children:x.title})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Kontekst dla AI *"}),e.jsx("textarea",{className:`mf-input mf-textarea${x.shortPrompt?" mf-input--err":""}`,value:u,onChange:d=>{k(d.target.value),j(N=>({...N,shortPrompt:""}))},placeholder:"Co chcesz wiedzieć o tym temacie? Np. wyjaśnij z przykładami kodu, skupiony na praktycznym użyciu...",rows:3}),x.shortPrompt&&e.jsx("span",{className:"mf-error",children:x.shortPrompt})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:Q.map(d=>e.jsx("button",{type:"button",className:`diff-btn${g===d.value?" diff-btn--on":""}`,style:g===d.value?{background:d.bg,borderColor:d.border,color:d.color}:{},onClick:()=>v(d.value),children:d.label},d.value))})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Typ notatki"}),e.jsxs("label",{className:"code-toggle",children:[e.jsx("input",{type:"checkbox",checked:f,onChange:d=>h(d.target.checked),className:"code-toggle-input"}),e.jsx("span",{className:`code-toggle-track${f?" code-toggle-track--on":""}`,children:e.jsx("span",{className:"code-toggle-thumb"})}),e.jsxs("div",{className:"code-toggle-labels",children:[e.jsx("span",{className:"code-toggle-main",children:f?e.jsxs(e.Fragment,{children:[e.jsx("svg",{width:"13",height:"13",viewBox:"0 0 13 13",fill:"none",style:{display:"inline-block",verticalAlign:"-2px",marginRight:"5px"},children:e.jsx("path",{d:"M9 2.5l2.5 4L9 11M4 2.5L1.5 6.5 4 11",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round",strokeLinejoin:"round"})}),"Tak — to jest KOD"]}):"Czy to KOD?"}),e.jsx("span",{className:"code-toggle-sub",children:f?'Odblokuje przycisk "Wygeneruj zadania" · nieodwracalne':"Zaznacz jeśli temat dotyczy programowania / kodu"})]})]})]}),e.jsxs("footer",{className:"tm-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:y,children:y?e.jsx("span",{className:"btn-spin"}):"Utwórz temat"})]})]})]}),e.jsx("style",{children:`
        .tm-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .tm-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:480px;
          max-height:90vh; overflow-y:auto;
          animation:fadeIn .2s ease both;
        }
        .tm-head { display:flex; align-items:flex-start; justify-content:space-between; padding:20px 20px 0; gap:12px; }
        .tm-eyebrow { font-family:var(--font-mono); font-size:0.6rem; font-weight:700; letter-spacing:0.12em; text-transform:uppercase; color:var(--accent); margin-bottom:2px; }
        .tm-title { font-size:1rem; font-weight:700; color:var(--text-0); }
        .tm-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; flex-shrink:0; transition:background var(--t-fast); }
        .tm-close:hover { background:var(--bg-2); }
        .tm-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
        .tm-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }

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

        .diff-group { display:flex; gap:6px; flex-wrap:wrap; }
        .diff-btn {
          flex:1; min-width:80px; padding:8px 6px; text-align:center;
          border-radius:var(--radius-md); border:1.5px solid var(--border-1);
          background:#fff; font-size:0.79rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all var(--t-fast);
        }
        .diff-btn:hover { border-color:var(--border-0); }
        .diff-btn--on { font-weight:700; }

        /* Toggle "Czy to KOD" */
        .code-toggle {
          display:flex; align-items:flex-start; gap:12px;
          padding:12px 14px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-2);
          cursor:pointer; transition:all var(--t-fast);
          user-select:none;
        }
        .code-toggle:hover { border-color:var(--border-0); }
        .code-toggle-input { display:none; }
        .code-toggle-track {
          width:36px; height:20px; border-radius:100px; flex-shrink:0;
          background:var(--bg-4); transition:background var(--t-mid);
          position:relative; margin-top:2px;
        }
        .code-toggle-track--on { background:var(--accent); }
        .code-toggle-thumb {
          position:absolute; top:3px; left:3px;
          width:14px; height:14px; border-radius:50%;
          background:#fff; transition:transform var(--t-mid);
          box-shadow:0 1px 3px rgba(0,0,0,0.2);
        }
        .code-toggle-track--on .code-toggle-thumb { transform:translateX(16px); }
        .code-toggle-labels { flex:1; }
        .code-toggle-main {
          display:block; font-size:0.84rem; font-weight:700;
          color:var(--text-0); margin-bottom:2px;
        }
        .code-toggle-sub { font-size:0.72rem; color:var(--text-2); line-height:1.4; }

        .btn-primary { padding:9px 20px; background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700; cursor:pointer; transition:background .15s; }
        .btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
        .btn-primary:disabled { opacity:.5; cursor:not-allowed; }
        .btn-ghost { padding:9px 16px; background:transparent; color:var(--text-2); border:1px solid var(--border-1); border-radius:var(--radius-md); font-size:0.84rem; cursor:pointer; transition:background .15s; }
        .btn-ghost:hover { background:var(--bg-2); }
        .btn-spin { display:inline-block; width:13px; height:13px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .6s linear infinite; vertical-align:middle; }
      `})]})}function oe(){R("Dashboard");const[t,a]=n.useState([]),[r,s]=n.useState(!0),[p,m]=n.useState(new Set),[u,k]=n.useState({}),[g,v]=n.useState(new Set),[f,h]=n.useState(null),[y,b]=n.useState([]),[x,j]=n.useState(!0),[i,l]=n.useState(null),[d,N]=n.useState(null),z=n.useCallback(async()=>{s(!0);try{const o=await A.getTree();a(o.data.children??[])}catch(o){o.status!==401&&C.error("Błąd ładowania kategorii")}finally{s(!1)}},[]);n.useEffect(()=>{z()},[z]);const P=n.useCallback(async o=>{if(m(c=>{const w=new Set(c);return w.has(o)?w.delete(o):w.add(o),w}),!u[o]){v(c=>new Set(c).add(o));try{const c=await T.listByCategory(o);k(w=>({...w,[o]:c.data}))}catch{}finally{v(c=>{const w=new Set(c);return w.delete(o),w})}}},[u]),E=n.useCallback(o=>{h(o),b(M(t,o.categoryId)),window.innerWidth<=860&&j(!1)},[t]),L=n.useCallback(o=>{z(),o.parentId&&m(c=>new Set(c).add(o.parentId))},[z]),B=n.useCallback(o=>{k(c=>({...c,[o.categoryId]:[...c[o.categoryId]??[],o]})),m(c=>new Set(c).add(o.categoryId)),h(o),b(M(t,o.categoryId)),window.innerWidth<=860&&j(!1)},[t]),W=n.useCallback(o=>{k(c=>({...c,[o.categoryId]:(c[o.categoryId]??[]).map(w=>w.id===o.id?o:w)})),h(c=>c?.id===o.id?o:c)},[]);return e.jsxs("div",{className:"db-root",children:[x&&e.jsx("div",{className:"db-overlay",onClick:()=>j(!1)}),e.jsx("div",{className:`db-tree-wrap${x?" db-tree-wrap--open":""}`,children:e.jsx(U,{tree:t,loading:r,expanded:p,topics:u,loadingTopics:g,selectedTopicId:f?.id,onToggle:P,onSelectTopic:E,onAddRootCategory:()=>l({parentId:null,parentName:null}),onAddSubcategory:(o,c)=>l({parentId:o,parentName:c}),onAddTopic:(o,c)=>N({categoryId:o,categoryName:c})})}),e.jsx(Y,{topic:f,categoryPath:y,onTopicUpdated:W,onOpenTree:()=>j(!0)}),i!==null&&e.jsx(X,{parentId:i.parentId,parentName:i.parentName,onClose:()=>l(null),onCreated:L}),d!==null&&e.jsx(ee,{categoryId:d.categoryId,categoryName:d.categoryName,onClose:()=>N(null),onCreated:B}),e.jsx("style",{children:`
        .db-root {
          display: flex;
          height: calc(100vh - 56px);
          margin: -28px -32px;
          overflow: hidden;
          position: relative;
        }

        .db-tree-wrap {
          flex-shrink: 0;
          display: flex;
        }

        /* ── Responsywność ── */
        @media (max-width: 860px) {
          .db-tree-wrap {
            position: absolute;
            top: 0; left: 0; bottom: 0;
            z-index: 50;
            transform: translateX(-100%);
            transition: transform var(--t-slow);
          }
          .db-tree-wrap--open {
            transform: translateX(0);
            box-shadow: var(--shadow-lg);
          }
          .db-overlay {
            position: absolute; inset: 0; z-index: 40;
            background: rgba(15,23,42,0.25);
            backdrop-filter: blur(1px);
          }
        }

        @media (min-width: 861px) {
          .db-overlay { display: none; }
        }
      `})]})}function M(t,a,r=[]){for(const s of t){const p=[...r,{id:s.id,name:s.name}];if(s.id===a)return p;if(s.children?.length){const m=M(s.children,a,p);if(m)return m}}return[]}export{oe as default};
