import{j as e,r as n,t as C}from"./index-DhcuM7Nn.js";import{u as R}from"./index-h3gy0YxQ.js";import{n as K,t as A,c as P}from"./services-D6-2Q08e.js";const $=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M3.5 2L7 5.5 3.5 9",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),F=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M2 3.5L5.5 7 9 3.5",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),H=()=>e.jsxs("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1",width:"9",height:"10",rx:"1.5",stroke:"currentColor",strokeWidth:"1.3"}),e.jsx("path",{d:"M3.5 4h5M3.5 6.5h5M3.5 9h3",stroke:"currentColor",strokeWidth:"1.2",strokeLinecap:"round"})]}),M=()=>e.jsx("svg",{width:"10",height:"10",viewBox:"0 0 10 10",fill:"none",children:e.jsx("path",{d:"M5 1.5v7M1.5 5h7",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),U={BASIC:"#16a34a",MEDIUM:"#d97706",HARD:"#dc2626"};function E({node:t,depth:a=0,expanded:r,topics:s,loadingTopics:l,selectedTopicId:m,onToggle:p,onSelectTopic:v,onAddSubcategory:h,onAddTopic:u}){const[f,g]=n.useState(!1),w=r.has(t.id),b=s[t.id]??[],k=l.has(t.id),j=a===0,i=a===1;return e.jsxs("div",{className:`tn-block tn-block--d${a}`,children:[e.jsxs("div",{className:`tn-row tn-row--d${a}${w?" tn-row--open":""}`,onMouseEnter:()=>g(!0),onMouseLeave:()=>g(!1),children:[e.jsx("button",{className:"tn-chevron",onClick:()=>p(t.id),children:w?e.jsx(F,{}):e.jsx($,{})}),e.jsx("span",{className:"tn-name",onClick:()=>p(t.id),title:t.name,children:t.name}),f&&e.jsxs("div",{className:"tn-actions",children:[j&&e.jsxs("button",{className:"tn-act tn-act--sub",title:"Dodaj podkategorię",onClick:d=>{d.stopPropagation(),h(t.id,t.name)},children:[e.jsx(M,{})," ",e.jsx("span",{children:"Podkat."})]}),i&&e.jsxs("button",{className:"tn-act tn-act--topic",title:"Dodaj temat / notatkę",onClick:d=>{d.stopPropagation(),u(t.id,t.name)},children:[e.jsx(M,{})," ",e.jsx("span",{children:"Temat"})]}),!j&&!i&&e.jsxs(e.Fragment,{children:[e.jsxs("button",{className:"tn-act tn-act--sub",onClick:d=>{d.stopPropagation(),h(t.id,t.name)},children:[e.jsx(M,{})," ",e.jsx("span",{children:"Podkat."})]}),e.jsxs("button",{className:"tn-act tn-act--topic",onClick:d=>{d.stopPropagation(),u(t.id,t.name)},children:[e.jsx(M,{})," ",e.jsx("span",{children:"Temat"})]})]})]})]}),w&&e.jsxs("div",{className:"tn-children",children:[(t.children??[]).map(d=>e.jsx(E,{node:d,depth:a+1,expanded:r,topics:s,loadingTopics:l,selectedTopicId:m,onToggle:p,onSelectTopic:v,onAddSubcategory:h,onAddTopic:u},d.id)),k&&e.jsxs("div",{className:"tn-loading",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}),!k&&b.map(d=>e.jsxs("button",{className:`tn-topic${m===d.id?" tn-topic--active":""}`,onClick:()=>v(d),children:[e.jsx("span",{className:"tn-topic-icon",children:e.jsx(H,{})}),e.jsx("span",{className:"tn-topic-name",title:d.title,children:d.title}),e.jsx("span",{className:"tn-topic-diff",title:d.difficulty,style:{background:U[d.difficulty]}})]},d.id)),!k&&b.length===0&&(t.children??[]).length===0&&e.jsx("p",{className:"tn-empty-hint",children:j?"Brak podkategorii":"Brak tematów"})]})]})}function _({tree:t,expanded:a,topics:r,loadingTopics:s,selectedTopicId:l,onToggle:m,onSelectTopic:p,onAddRootCategory:v,onAddSubcategory:h,onAddTopic:u,loading:f}){return e.jsxs("aside",{className:"tree-panel",children:[e.jsxs("div",{className:"tree-head",children:[e.jsx("span",{className:"tree-head-title",children:"Baza wiedzy"}),e.jsx("button",{className:"tree-add-btn",onClick:v,title:"Nowa kategoria (np. IT, HISTORIA)",children:e.jsx(M,{})})]}),e.jsxs("div",{className:"tree-legend",children:[e.jsx("span",{className:"tree-legend-item tree-legend-item--cat",children:"Kategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--sub",children:"Podkategoria"}),e.jsx("span",{className:"tree-legend-sep",children:"›"}),e.jsx("span",{className:"tree-legend-item tree-legend-item--topic",children:"Temat"})]}),e.jsx("div",{className:"tree-body",children:f?e.jsxs("div",{className:"tree-status",children:[e.jsx("span",{className:"tn-spinner"})," Ładowanie..."]}):t.length===0?e.jsxs("div",{className:"tree-empty",children:[e.jsx("svg",{width:"36",height:"36",viewBox:"0 0 36 36",fill:"none",style:{color:"var(--border-1)"},children:e.jsx("path",{d:"M4 10a2 2 0 012-2h8l2 2h14a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V10z",stroke:"currentColor",strokeWidth:"1.5"})}),e.jsx("p",{className:"tree-empty-title",children:"Brak kategorii"}),e.jsxs("p",{className:"tree-empty-sub",children:["Zacznij od dodania kategorii",e.jsx("br",{}),"np. ",e.jsx("em",{children:"IT"})," lub ",e.jsx("em",{children:"HISTORIA"})]}),e.jsx("button",{className:"tree-empty-btn",onClick:v,children:"Dodaj kategorię →"})]}):t.map(g=>e.jsx(E,{node:g,depth:0,expanded:a,topics:r,loadingTopics:s,selectedTopicId:l,onToggle:m,onSelectTopic:p,onAddSubcategory:h,onAddTopic:u},g.id))}),e.jsx("style",{children:`
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
      `})]})}function V({topicTitle:t,onClose:a}){return e.jsxs("div",{className:"modal-backdrop",onClick:a,children:[e.jsxs("div",{className:"modal-box",onClick:r=>r.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:"AI · Dopytaj o temat"}),e.jsx("h2",{className:"modal-title",children:t})]}),e.jsx("button",{className:"modal-close",onClick:a,children:"✕"})]}),e.jsxs("div",{className:"modal-body",children:[e.jsx("p",{className:"modal-info",children:"Zadaj dodatkowe pytanie dotyczące tej notatki. AI uzupełni lub rozszerzy treść na podstawie Twojego pytania."}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Twoje pytanie"}),e.jsx("textarea",{className:"mf-input mf-textarea",placeholder:"np. Wyjaśnij prościej, dodaj przykład z życia codziennego, pokaż jak to działa w Spring Boot...",rows:4,disabled:!0})]}),e.jsxs("div",{className:"modal-placeholder",children:[e.jsx("span",{className:"modal-placeholder-icon",children:"🔧"}),e.jsx("span",{children:"Integracja AI w przygotowaniu"})]})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{className:"btn-ghost",onClick:a,children:"Zamknij"}),e.jsx("button",{className:"btn-primary",disabled:!0,title:"Wkrótce dostępne",children:"Wyślij pytanie →"})]})]}),e.jsx("style",{children:`
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
      `})]})}const Z=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}],Y={NEW:"Nowy",NOTE_GENERATED:"Notatka gotowa",QUIZ_READY:"Quiz dostępny",PASSED:"Zaliczony",MASTERED:"Opanowany"};function q({topic:t,categoryPath:a,onTopicUpdated:r,onOpenTree:s}){const[l,m]=n.useState(null),[p,v]=n.useState(!1),[h,u]=n.useState(t?.difficulty??"BASIC"),[f,g]=n.useState(!1),[w,b]=n.useState(!1);n.useEffect(()=>{if(!t){m(null);return}u(t.difficulty),v(!0),K.getByTopic(t.id).then(i=>m(i.data)).catch(i=>{i.status===404?m(null):C.error("Błąd ładowania notatki")}).finally(()=>v(!1))},[t?.id]);const k=async i=>{if(!(i===h||f)){u(i),g(!0);try{const d=await A.update(t.id,{difficulty:i});r?.(d.data)}catch{C.error("Błąd zapisu trudności"),u(t.difficulty)}finally{g(!1)}}};if(!t)return e.jsxs("div",{className:"np-empty",children:[e.jsxs("button",{className:"np-tree-toggle",onClick:s,title:"Otwórz drzewo kategorii",children:[e.jsx("svg",{width:"18",height:"18",viewBox:"0 0 18 18",fill:"none",children:e.jsx("path",{d:"M3 5h12M3 9h8M3 13h10",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),"Kategorie"]}),e.jsxs("div",{className:"np-empty-inner",children:[e.jsxs("svg",{width:"52",height:"52",viewBox:"0 0 52 52",fill:"none",children:[e.jsx("rect",{x:"8",y:"6",width:"36",height:"40",rx:"4",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M16 18h20M16 25h20M16 32h12",stroke:"var(--border-1)",strokeWidth:"1.8",strokeLinecap:"round"})]}),e.jsx("h2",{className:"np-empty-title",children:"Wybierz temat"}),e.jsxs("p",{className:"np-empty-sub",children:["Kliknij temat z drzewa kategorii,",e.jsx("br",{}),"aby wyświetlić lub stworzyć notatkę."]})]}),e.jsx(L,{})]});const j=t.code??!1;return e.jsxs("div",{className:"note-panel",children:[e.jsxs("div",{className:"note-header",children:[e.jsxs("div",{className:"note-header-top",children:[e.jsx("button",{className:"np-tree-toggle np-tree-toggle--inline",onClick:s,title:"Otwórz drzewo",children:e.jsx("svg",{width:"16",height:"16",viewBox:"0 0 16 16",fill:"none",children:e.jsx("path",{d:"M2 4.5h12M2 8.5h8M2 12.5h10",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round"})})}),e.jsx("div",{className:"note-breadcrumb",children:(a??[]).map((i,d)=>e.jsxs("span",{children:[d>0&&e.jsx("span",{className:"bc-sep",children:"›"}),e.jsx("span",{className:"bc-item",children:i.name})]},i.id))}),e.jsxs("div",{className:"note-badges",children:[e.jsx("span",{className:"note-status","data-status":t.status,children:Y[t.status]??t.status}),j&&e.jsxs("span",{className:"note-code-badge",children:[e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M7.5 2l3 3.5-3 3.5M3.5 2l-3 3.5 3 3.5",stroke:"currentColor",strokeWidth:"1.4",strokeLinecap:"round",strokeLinejoin:"round"})}),"KOD"]})]})]}),e.jsx("h1",{className:"note-title",children:t.title})]}),e.jsxs("div",{className:"note-config",children:[j&&e.jsxs("div",{className:"note-code-banner",children:[e.jsx("span",{className:"note-code-banner-icon",children:e.jsx("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:e.jsx("path",{d:"M9.5 2.5l3 4-3 4M4.5 2.5l-3 4 3 4",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round",strokeLinejoin:"round"})})}),e.jsxs("div",{children:[e.jsx("span",{className:"note-code-banner-title",children:"Notatka oznaczona jako KOD"}),e.jsx("span",{className:"note-code-banner-sub",children:'AI wygeneruje treść z przykładami kodu · dostępny przycisk "Wygeneruj zadania"'})]})]}),e.jsxs("div",{className:"note-config-row",children:[e.jsx("span",{className:"note-config-label",children:"Trudność"}),e.jsx("div",{className:"diff-group",children:Z.map(i=>e.jsxs("button",{className:`diff-btn${h===i.value?" diff-btn--on":""}`,style:h===i.value?{background:i.bg,borderColor:i.border,color:i.color}:{},onClick:()=>k(i.value),disabled:f,children:[i.label,h===i.value&&f&&e.jsx("span",{className:"diff-saving"})]},i.value))})]})]}),e.jsx("div",{className:"note-content-area",children:p?e.jsxs("div",{className:"note-loading",children:[e.jsx("span",{className:"note-spinner"})," Ładowanie notatki..."]}):l?e.jsx(G,{note:l}):e.jsx(J,{})}),e.jsxs("div",{className:"note-actions",children:[j&&e.jsxs("button",{className:"act-btn act-btn--code",disabled:!0,title:"Wkrótce — wymaga AI",children:[e.jsx("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:e.jsx("path",{d:"M9.5 2.5l3 4-3 4M4.5 2.5l-3 4 3 4",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round",strokeLinejoin:"round"})}),"Wygeneruj zadania"]}),e.jsx("div",{className:"act-spacer"}),e.jsxs("button",{className:"act-btn act-btn--ask",onClick:()=>b(!0),children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("circle",{cx:"7",cy:"7",r:"6",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M5.3 5.5C5.5 4.6 6.1 4 7 4c.9 0 1.7.7 1.7 1.5 0 .9-.9 1.4-1.7 1.6v.9",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"}),e.jsx("circle",{cx:"7",cy:"10.5",r:"0.6",fill:"currentColor"})]}),"Dopytaj o..."]}),e.jsxs("button",{className:"act-btn act-btn--quiz",disabled:!0,title:"Wkrótce dostępne",children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1.5",width:"11",height:"11",rx:"2",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M4 4.5h6M4 7h6M4 9.5h4",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"})]}),"Rozpocznij quiz"]})]}),w&&e.jsx(V,{topicTitle:t.title,onClose:()=>b(!1)}),e.jsx(L,{})]})}function J(){return e.jsxs("div",{className:"note-empty-content",children:[e.jsxs("div",{className:"nec-top",children:[e.jsxs("svg",{width:"44",height:"44",viewBox:"0 0 44 44",fill:"none",children:[e.jsx("circle",{cx:"22",cy:"22",r:"19",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M14 22h16M22 14v16",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]}),e.jsx("p",{className:"nec-title",children:"Brak notatki"}),e.jsxs("p",{className:"nec-sub",children:["Treść zostanie wygenerowana przez AI.",e.jsx("br",{}),"Integracja w przygotowaniu."]})]}),e.jsx("div",{className:"nec-skeleton",children:[90,75,83,60,88,70,55,78].map((t,a)=>e.jsx("span",{className:"nec-line",style:{width:`${t}%`,animationDelay:`${a*.12}s`}},a))})]})}const X={FOR_CHILD:{text:"Dla ośmiolatka",color:"#16a34a",bg:"#f0fdf4"},CONTRASTIVE:{text:"Kontrastowy",color:"#d97706",bg:"#fffbeb"},ADVANCED:{text:"Zaawansowany",color:"#7c3aed",bg:"#f5f3ff"}};function G({note:t}){const a=t.content;return a?e.jsxs("article",{className:"note-article",children:[a.title&&e.jsx("h2",{className:"na-title",children:a.title}),a.summary&&e.jsx("p",{className:"na-summary",children:a.summary}),a.simpleExplanation&&e.jsxs("div",{className:"na-simple",children:[e.jsx("span",{className:"na-simple-label",children:"Jak dla dziecka"}),e.jsx("p",{children:a.simpleExplanation})]}),(a.sections??[]).map((r,s)=>e.jsxs("section",{className:"na-section",children:[e.jsx("h3",{className:"na-section-h",children:r.heading}),e.jsx("p",{className:"na-section-p",children:r.content})]},s)),(a.examples??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Przykłady"}),a.examples.map((r,s)=>{const l=X[r.level];return e.jsxs("div",{className:"na-example",children:[e.jsxs("div",{className:"na-example-head",children:[r.title&&e.jsx("span",{className:"na-example-title",children:r.title}),l&&e.jsx("span",{className:"na-example-badge",style:{color:l.color,background:l.bg,border:`1px solid ${l.color}30`},children:l.text})]}),r.content&&e.jsx("pre",{className:"na-code",children:e.jsx("code",{children:r.content})}),r.explanation&&e.jsx("p",{className:"na-example-exp",children:r.explanation})]},s)})]}),(a.memoryPoints??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Do zapamiętania"}),e.jsx("ul",{className:"na-list na-list--green",children:a.memoryPoints.map((r,s)=>e.jsx("li",{children:r},s))})]}),(a.commonMistakes??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Częste błędy"}),e.jsx("ul",{className:"na-list na-list--red",children:a.commonMistakes.map((r,s)=>e.jsx("li",{children:r},s))})]}),(a.suggestedSearchPhrases??[]).length>0&&e.jsxs(e.Fragment,{children:[e.jsx("h3",{className:"na-sub-h",children:"Wyszukaj więcej"}),e.jsx("ul",{className:"na-list na-list--search",children:a.suggestedSearchPhrases.map((r,s)=>e.jsx("li",{children:e.jsx("code",{className:"na-search-phrase",children:r})},s))})]})]}):null}function L(){return e.jsx("style",{children:`
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
        display:flex; flex-direction:column; gap:10px;
      }

      /* Baner KOD */
      .note-code-banner {
        display:flex; align-items:flex-start; gap:10px;
        padding:10px 12px; border-radius:var(--radius-md);
        background:#eff6ff; border:1px solid rgba(37,99,235,.25);
      }
      .note-code-banner-icon {
        color:var(--accent); flex-shrink:0; display:flex; margin-top:1px;
      }
      .note-code-banner-title {
        display:block; font-size:0.78rem; font-weight:700; color:var(--accent);
        margin-bottom:2px;
      }
      .note-code-banner-sub {
        display:block; font-size:0.71rem; color:var(--text-2); line-height:1.5;
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
      .na-example-head { display:flex; align-items:center; gap:8px; margin-bottom:7px; flex-wrap:wrap; }
      .na-example-title { font-size:0.8rem; font-weight:700; color:var(--text-1); }
      .na-example-badge {
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:2px 7px; border-radius:100px;
      }
      .na-code { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.79rem; color:var(--text-0); margin:7px 0; white-space:pre-wrap; word-break:break-word; }
      .na-example-exp { font-size:0.79rem; color:var(--text-2); margin-top:5px; line-height:1.6; }
      .na-list--search { list-style:none; padding-left:0; }
      .na-search-phrase { font-family:var(--font-mono); font-size:0.78rem; background:var(--bg-2); padding:2px 7px; border-radius:4px; color:var(--accent); }
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
    `})}function Q({parentId:t,parentName:a,onClose:r,onCreated:s}){const[l,m]=n.useState(""),[p,v]=n.useState(""),[h,u]=n.useState(!1),[f,g]=n.useState(""),w=async b=>{if(b.preventDefault(),!l.trim()){g("Nazwa jest wymagana");return}u(!0),g("");try{const k=await P.create({name:l.trim(),description:p.trim()||null,parentId:t??null});C.success(`Kategoria "${k.data.name}" utworzona`),s(k.data),r()}catch(k){g(k.message??"Błąd tworzenia kategorii")}finally{u(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:r,children:[e.jsxs("div",{className:"modal-box",onClick:b=>b.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:t?"Podkategoria":"Kategoria główna"}),e.jsx("h2",{className:"modal-title",children:t?`w "${a}"`:"Nowa kategoria"})]}),e.jsx("button",{className:"modal-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:w,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Nazwa *"}),e.jsx("input",{className:"mf-input",value:l,onChange:b=>{m(b.target.value),g("")},placeholder:t?"np. JAVA, SIECI, RENESANS":"np. IT, HISTORIA, MEDYCYNA",autoFocus:!0}),f&&e.jsx("span",{className:"mf-error",children:f})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Opis (opcjonalny)"}),e.jsx("input",{className:"mf-input",value:p,onChange:b=>v(b.target.value),placeholder:"Krótki opis kategorii"})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:h,children:h?e.jsx("span",{className:"btn-spin"}):"Utwórz"})]})]})]}),e.jsx(ee,{})]})}function ee(){return e.jsx("style",{children:`
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
    `})}const D="POTWIERDŹ";function te({onConfirm:t,onClose:a}){const[r,s]=n.useState(""),l=r===D,m=p=>{p.preventDefault(),l&&(t(),a())};return e.jsxs("div",{className:"ccm-backdrop",onClick:a,children:[e.jsxs("div",{className:"ccm-box",onClick:p=>p.stopPropagation(),children:[e.jsx("div",{className:"ccm-icon-wrap",children:e.jsxs("svg",{width:"28",height:"28",viewBox:"0 0 28 28",fill:"none",children:[e.jsx("path",{d:"M8 5l6 10 6-10",stroke:"white",strokeWidth:"1.8",strokeLinecap:"round",strokeLinejoin:"round"}),e.jsx("path",{d:"M3 21l9-16a2.3 2.3 0 014 0l9 16a2 2 0 01-1.7 3H4.7A2 2 0 013 21z",stroke:"white",strokeWidth:"1.8",strokeLinejoin:"round",fill:"none"}),e.jsx("circle",{cx:"14",cy:"20",r:"1",fill:"white"}),e.jsx("path",{d:"M14 13v4",stroke:"white",strokeWidth:"1.8",strokeLinecap:"round"})]})}),e.jsx("h2",{className:"ccm-title",children:"Oznaczenie notatki jako KOD"}),e.jsxs("div",{className:"ccm-info",children:[e.jsxs("p",{className:"ccm-info-text",children:["Oznaczenie tematu jako ",e.jsx("strong",{children:"KOD"})," jest ",e.jsx("strong",{children:"nieodwracalne"}),". Po zapisaniu nie będzie możliwości zmiany tej opcji."]}),e.jsxs("ul",{className:"ccm-info-list",children:[e.jsx("li",{children:"Notatka zostanie sklasyfikowana jako materiał programistyczny"}),e.jsxs("li",{children:["Odblokujesz przycisk ",e.jsx("strong",{children:'"Wygeneruj zadania"'})]}),e.jsx("li",{children:"AI będzie generować treści zorientowane na kod"})]})]}),e.jsxs("form",{onSubmit:m,className:"ccm-form",children:[e.jsxs("label",{className:"ccm-field-label",children:["Wpisz ",e.jsx("code",{className:"ccm-code",children:D})," aby potwierdzić:"]}),e.jsx("input",{className:`ccm-input${r.length>0&&!l?" ccm-input--wrong":""}${l?" ccm-input--ok":""}`,value:r,onChange:p=>s(p.target.value.toUpperCase()),placeholder:D,autoFocus:!0,autoComplete:"off",spellCheck:!1}),r.length>0&&!l&&e.jsxs("span",{className:"ccm-hint",children:["Wpisz dokładnie: ",D]}),e.jsxs("div",{className:"ccm-foot",children:[e.jsx("button",{type:"button",className:"ccm-btn-cancel",onClick:a,children:"Anuluj"}),e.jsxs("button",{type:"submit",className:"ccm-btn-confirm",disabled:!l,children:[e.jsx("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:e.jsx("path",{d:"M2 7l4 4 6-7",stroke:"white",strokeWidth:"1.8",strokeLinecap:"round",strokeLinejoin:"round"})}),"Potwierdzam — to jest KOD"]})]})]})]}),e.jsx("style",{children:`
        .ccm-backdrop {
          position:fixed; inset:0; z-index:300;
          background:rgba(15,23,42,0.55); backdrop-filter:blur(4px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .ccm-box {
          background:#fff; border-radius:var(--radius-xl);
          border:1px solid rgba(220,38,38,.2);
          box-shadow:0 0 0 4px rgba(220,38,38,.06), var(--shadow-lg);
          width:100%; max-width:420px;
          animation:fadeIn .2s ease both;
          overflow:hidden;
        }

        .ccm-icon-wrap {
          background:linear-gradient(135deg, #dc2626, #b91c1c);
          padding:20px; display:flex; align-items:center; justify-content:center;
        }

        .ccm-title {
          font-size:1rem; font-weight:800; color:var(--text-0);
          padding:16px 20px 0; margin:0;
        }

        .ccm-info {
          padding:12px 20px 0;
        }
        .ccm-info-text {
          font-size:0.82rem; color:var(--text-1); line-height:1.6; margin-bottom:10px;
        }
        .ccm-info-text strong { color:var(--text-0); }
        .ccm-info-list {
          margin:0; padding-left:18px;
          display:flex; flex-direction:column; gap:4px;
        }
        .ccm-info-list li {
          font-size:0.78rem; color:var(--text-2); line-height:1.5;
        }
        .ccm-info-list li strong { color:var(--text-1); }

        .ccm-form {
          padding:16px 20px 20px;
          display:flex; flex-direction:column; gap:8px;
        }
        .ccm-field-label {
          font-size:0.76rem; color:var(--text-2); line-height:1.5;
        }
        .ccm-code {
          font-family:var(--font-mono); font-size:0.78rem; font-weight:700;
          color:var(--red); background:var(--red-light);
          padding:1px 5px; border-radius:4px; border:1px solid rgba(220,38,38,.15);
        }
        .ccm-input {
          width:100%; padding:10px 13px;
          border-radius:var(--radius-md);
          border:2px solid var(--border-1); background:var(--bg-2);
          font-family:var(--font-mono); font-size:0.9rem; font-weight:700;
          color:var(--text-0); outline:none; letter-spacing:0.05em;
          transition:border-color .15s, background .15s;
          text-transform:uppercase;
        }
        .ccm-input:focus { border-color:var(--accent); background:#fff; }
        .ccm-input--wrong { border-color:var(--red) !important; background:var(--red-light) !important; }
        .ccm-input--ok    { border-color:#16a34a !important; background:#f0fdf4 !important; }
        .ccm-hint { font-size:0.7rem; color:var(--red); }

        .ccm-foot {
          display:flex; gap:8px; margin-top:4px;
        }
        .ccm-btn-cancel {
          flex:1; padding:10px; background:transparent; border:1px solid var(--border-1);
          border-radius:var(--radius-md); font-size:0.83rem; color:var(--text-2);
          cursor:pointer; transition:background .15s;
        }
        .ccm-btn-cancel:hover { background:var(--bg-2); }
        .ccm-btn-confirm {
          flex:2; padding:10px 14px;
          background:linear-gradient(135deg, #dc2626, #b91c1c);
          color:#fff; border:none; border-radius:var(--radius-md);
          font-size:0.83rem; font-weight:700;
          cursor:pointer; display:flex; align-items:center; justify-content:center; gap:6px;
          transition:filter .15s, opacity .15s;
        }
        .ccm-btn-confirm:hover:not(:disabled) { filter:brightness(1.08); }
        .ccm-btn-confirm:disabled { opacity:.35; cursor:not-allowed; }
      `})]})}const ae=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}];function re({categoryId:t,categoryName:a,onClose:r,onCreated:s}){const[l,m]=n.useState(""),[p,v]=n.useState(""),[h,u]=n.useState("BASIC"),[f,g]=n.useState(!1),[w,b]=n.useState(!1),[k,j]=n.useState(!1),[i,d]=n.useState({}),S=()=>{const c={};return l.trim()||(c.title="Tytuł jest wymagany"),p.trim()||(c.shortPrompt="Kontekst jest wymagany"),c},I=async c=>{c.preventDefault();const N=S();if(Object.keys(N).length){d(N);return}j(!0);try{const z=await A.create({categoryId:t,title:l.trim(),shortPrompt:p.trim(),difficulty:h,code:f});C.success(`Temat "${z.data.title}" utworzony`),s(z.data),r()}catch(z){C.error(z.message??"Błąd tworzenia tematu")}finally{j(!1)}};return e.jsxs("div",{className:"tm-backdrop",onClick:r,children:[e.jsxs("div",{className:"tm-box",onClick:c=>c.stopPropagation(),children:[e.jsxs("header",{className:"tm-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"tm-eyebrow",children:"Nowy temat"}),e.jsxs("h2",{className:"tm-title",children:['w "',a,'"']})]}),e.jsx("button",{className:"tm-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:I,className:"tm-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Tytuł tematu *"}),e.jsx("input",{className:`mf-input${i.title?" mf-input--err":""}`,value:l,onChange:c=>{m(c.target.value),d(N=>({...N,title:""}))},placeholder:"np. Polimorfizm w Javie",autoFocus:!0}),i.title&&e.jsx("span",{className:"mf-error",children:i.title})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Kontekst dla AI *"}),e.jsx("textarea",{className:`mf-input mf-textarea${i.shortPrompt?" mf-input--err":""}`,value:p,onChange:c=>{v(c.target.value),d(N=>({...N,shortPrompt:""}))},placeholder:"Co chcesz wiedzieć o tym temacie? Np. wyjaśnij z przykładami kodu, skupiony na praktycznym użyciu...",rows:3}),i.shortPrompt&&e.jsx("span",{className:"mf-error",children:i.shortPrompt})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:ae.map(c=>e.jsx("button",{type:"button",className:`diff-btn${h===c.value?" diff-btn--on":""}`,style:h===c.value?{background:c.bg,borderColor:c.border,color:c.color}:{},onClick:()=>u(c.value),children:c.label},c.value))})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Typ notatki"}),e.jsxs("label",{className:"code-toggle",onClick:c=>{f||(c.preventDefault(),b(!0))},children:[e.jsx("input",{type:"checkbox",checked:f,onChange:c=>{c.target.checked||g(!1)},className:"code-toggle-input"}),e.jsx("span",{className:`code-toggle-track${f?" code-toggle-track--on":""}`,children:e.jsx("span",{className:"code-toggle-thumb"})}),e.jsxs("div",{className:"code-toggle-labels",children:[e.jsx("span",{className:"code-toggle-main",children:f?e.jsxs(e.Fragment,{children:[e.jsx("svg",{width:"13",height:"13",viewBox:"0 0 13 13",fill:"none",style:{display:"inline-block",verticalAlign:"-2px",marginRight:"5px"},children:e.jsx("path",{d:"M9 2.5l2.5 4L9 11M4 2.5L1.5 6.5 4 11",stroke:"currentColor",strokeWidth:"1.5",strokeLinecap:"round",strokeLinejoin:"round"})}),"Tak — to jest KOD"]}):"Czy to KOD?"}),e.jsx("span",{className:"code-toggle-sub",children:f?'Odblokuje przycisk "Wygeneruj zadania" · nieodwracalne':"Zaznacz jeśli temat dotyczy programowania / kodu"})]})]})]}),e.jsxs("footer",{className:"tm-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:k,children:k?e.jsx("span",{className:"btn-spin"}):"Utwórz temat"})]})]})]}),w&&e.jsx(te,{onConfirm:()=>g(!0),onClose:()=>b(!1)}),e.jsx("style",{children:`
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
      `})]})}function se(){R("Dashboard");const[t,a]=n.useState([]),[r,s]=n.useState(!0),[l,m]=n.useState(new Set),[p,v]=n.useState({}),[h,u]=n.useState(new Set),[f,g]=n.useState(null),[w,b]=n.useState([]),[k,j]=n.useState(!0),[i,d]=n.useState(null),[S,I]=n.useState(null),c=n.useCallback(async()=>{s(!0);try{const o=await P.getTree();a(o.data.children??[])}catch(o){o.status!==401&&C.error("Błąd ładowania kategorii")}finally{s(!1)}},[]);n.useEffect(()=>{c()},[c]);const N=n.useCallback(async o=>{if(m(x=>{const y=new Set(x);return y.has(o)?y.delete(o):y.add(o),y}),!p[o]){u(x=>new Set(x).add(o));try{const x=await A.listByCategory(o);v(y=>({...y,[o]:x.data}))}catch{}finally{u(x=>{const y=new Set(x);return y.delete(o),y})}}},[p]),z=n.useCallback(o=>{g(o),b(T(t,o.categoryId)),window.innerWidth<=860&&j(!1)},[t]),W=n.useCallback(o=>{c(),o.parentId&&m(x=>new Set(x).add(o.parentId))},[c]),B=n.useCallback(o=>{v(x=>({...x,[o.categoryId]:[...x[o.categoryId]??[],o]})),m(x=>new Set(x).add(o.categoryId)),g(o),b(T(t,o.categoryId)),window.innerWidth<=860&&j(!1)},[t]),O=n.useCallback(o=>{v(x=>({...x,[o.categoryId]:(x[o.categoryId]??[]).map(y=>y.id===o.id?o:y)})),g(x=>x?.id===o.id?o:x)},[]);return e.jsxs("div",{className:"db-root",children:[k&&e.jsx("div",{className:"db-overlay",onClick:()=>j(!1)}),e.jsx("div",{className:`db-tree-wrap${k?" db-tree-wrap--open":""}`,children:e.jsx(_,{tree:t,loading:r,expanded:l,topics:p,loadingTopics:h,selectedTopicId:f?.id,onToggle:N,onSelectTopic:z,onAddRootCategory:()=>d({parentId:null,parentName:null}),onAddSubcategory:(o,x)=>d({parentId:o,parentName:x}),onAddTopic:(o,x)=>I({categoryId:o,categoryName:x})})}),e.jsx(q,{topic:f,categoryPath:w,onTopicUpdated:O,onOpenTree:()=>j(!0)}),i!==null&&e.jsx(Q,{parentId:i.parentId,parentName:i.parentName,onClose:()=>d(null),onCreated:W}),S!==null&&e.jsx(re,{categoryId:S.categoryId,categoryName:S.categoryName,onClose:()=>I(null),onCreated:B}),e.jsx("style",{children:`
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
      `})]})}function T(t,a,r=[]){for(const s of t){const l=[...r,{id:s.id,name:s.name}];if(s.id===a)return l;if(s.children?.length){const m=T(s.children,a,l);if(m)return m}}return[]}export{se as default};
