import{j as e,r as i,t as N}from"./index-BexZMbGE.js";import{u as W}from"./index-Z7pE7Zxz.js";import{n as R,t as S,c as I}from"./services-D6-2Q08e.js";const U=()=>e.jsx("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:e.jsx("path",{d:"M4 2.5L7.5 6 4 9.5",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),O=()=>e.jsx("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:e.jsx("path",{d:"M2 4L6 8l4-4",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),F=({open:t})=>e.jsx("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:t?e.jsx("path",{d:"M1 4.5a1 1 0 011-1h3l1 1h6a1 1 0 011 1v5a1 1 0 01-1 1H2a1 1 0 01-1-1v-6z",fill:"var(--accent)",opacity:"0.7",stroke:"none"}):e.jsx("path",{d:"M1 4.5a1 1 0 011-1h3l1 1h6a1 1 0 011 1v5a1 1 0 01-1 1H2a1 1 0 01-1-1v-6z",stroke:"currentColor",strokeWidth:"1.3",fill:"none"})}),K=()=>e.jsxs("svg",{width:"12",height:"12",viewBox:"0 0 12 12",fill:"none",children:[e.jsx("rect",{x:"1.5",y:"1",width:"9",height:"10",rx:"1.5",stroke:"currentColor",strokeWidth:"1.3"}),e.jsx("path",{d:"M3.5 4h5M3.5 6.5h5M3.5 9h3",stroke:"currentColor",strokeWidth:"1.2",strokeLinecap:"round"})]}),z=()=>e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M5.5 1.5v8M1.5 5.5h8",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round"})}),$={BASIC:"#16a34a",MEDIUM:"#d97706",HARD:"#dc2626"},H={BASIC:"●",MEDIUM:"●",HARD:"●"};function D({node:t,depth:n=0,expanded:r,topics:l,loadingTopics:c,selectedTopicId:u,onToggle:x,onSelectTopic:m,onAddSubcategory:f,onAddTopic:b}){const[j,p]=i.useState(!1),h=r.has(t.id),g=(t.children?.length??0)>0,v=l[t.id]??[],k=c.has(t.id),a=n*16;return e.jsxs("div",{children:[e.jsxs("div",{className:`tree-row${h?" tree-row--open":""}`,style:{paddingLeft:12+a},onMouseEnter:()=>p(!0),onMouseLeave:()=>p(!1),children:[e.jsx("button",{className:"tree-chevron",onClick:()=>x(t.id),"aria-label":h?"Zwiń":"Rozwiń",children:h?e.jsx(O,{}):e.jsx(U,{})}),e.jsx("span",{className:"tree-folder-icon",children:e.jsx(F,{open:h})}),e.jsx("span",{className:"tree-node-name",onClick:()=>x(t.id),title:t.name,children:t.name}),j&&e.jsxs("div",{className:"tree-actions",children:[e.jsxs("button",{className:"tree-action-btn",title:"Dodaj podkategorię",onClick:s=>{s.stopPropagation(),f(t.id,t.name)},children:[e.jsx("span",{className:"tree-action-icon",children:e.jsx(z,{})}),e.jsx("span",{className:"tree-action-text",children:"Kat."})]}),e.jsxs("button",{className:"tree-action-btn tree-action-btn--topic",title:"Dodaj temat",onClick:s=>{s.stopPropagation(),b(t.id,t.name)},children:[e.jsx("span",{className:"tree-action-icon",children:e.jsx(z,{})}),e.jsx("span",{className:"tree-action-text",children:"Temat"})]})]})]}),h&&e.jsxs("div",{children:[(t.children??[]).map(s=>e.jsx(D,{node:s,depth:n+1,expanded:r,topics:l,loadingTopics:c,selectedTopicId:u,onToggle:x,onSelectTopic:m,onAddSubcategory:f,onAddTopic:b},s.id)),k&&e.jsxs("div",{className:"tree-loading",style:{paddingLeft:28+a+16},children:[e.jsx("span",{className:"tree-spinner"})," Ładowanie..."]}),v.map(s=>e.jsxs("button",{className:`tree-topic${u===s.id?" tree-topic--active":""}`,style:{paddingLeft:28+a+16},onClick:()=>m(s),children:[e.jsx("span",{className:"tree-topic-icon",children:e.jsx(K,{})}),e.jsx("span",{className:"tree-topic-name",title:s.title,children:s.title}),e.jsx("span",{className:"tree-topic-dot",title:s.difficulty,style:{color:$[s.difficulty]},children:H[s.difficulty]})]},s.id)),!k&&v.length===0&&!g&&e.jsx("p",{className:"tree-empty",style:{paddingLeft:28+a+16},children:"Brak tematów"})]})]})}function Z({tree:t,expanded:n,topics:r,loadingTopics:l,selectedTopicId:c,onToggle:u,onSelectTopic:x,onAddRootCategory:m,onAddSubcategory:f,onAddTopic:b,loading:j}){return e.jsxs("aside",{className:"tree-panel",children:[e.jsxs("div",{className:"tree-header",children:[e.jsx("span",{className:"tree-header-title",children:"Baza wiedzy"}),e.jsx("button",{className:"tree-add-root",onClick:m,title:"Nowa kategoria",children:e.jsx(z,{})})]}),e.jsx("div",{className:"tree-content",children:j?e.jsxs("div",{className:"tree-loading-full",children:[e.jsx("span",{className:"tree-spinner"})," Ładowanie..."]}):t.length===0?e.jsxs("div",{className:"tree-empty-full",children:[e.jsx("svg",{width:"32",height:"32",viewBox:"0 0 32 32",fill:"none",style:{color:"var(--text-3)",margin:"0 auto 8px"},children:e.jsx("path",{d:"M4 10a2 2 0 012-2h7l2 2h11a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V10z",stroke:"currentColor",strokeWidth:"1.5",fill:"none"})}),e.jsx("p",{children:"Brak kategorii"}),e.jsx("button",{className:"tree-create-first",onClick:m,children:"Utwórz pierwszą →"})]}):t.map(p=>e.jsx(D,{node:p,depth:0,expanded:n,topics:r,loadingTopics:l,selectedTopicId:c,onToggle:u,onSelectTopic:x,onAddSubcategory:f,onAddTopic:b},p.id))}),e.jsx("style",{children:`
        .tree-panel {
          width: 272px; flex-shrink: 0;
          border-right: 1px solid var(--border-1);
          background: #fff;
          display: flex; flex-direction: column;
          height: 100%;
        }
        .tree-header {
          display: flex; align-items: center; justify-content: space-between;
          padding: 14px 14px 10px;
          border-bottom: 1px solid var(--border-1);
          flex-shrink: 0;
        }
        .tree-header-title {
          font-size: 0.72rem; font-weight: 700;
          letter-spacing: 0.1em; text-transform: uppercase;
          color: var(--text-3);
        }
        .tree-add-root {
          width: 26px; height: 26px; border-radius: 6px;
          background: var(--accent-light); border: 1px solid var(--border-0);
          color: var(--accent); display: flex; align-items: center; justify-content: center;
          cursor: pointer; transition: background var(--t-fast);
        }
        .tree-add-root:hover { background: var(--accent-glow-strong); }

        .tree-content {
          flex: 1; overflow-y: auto; padding: 6px 0;
        }

        /* Wiersz kategorii */
        .tree-row {
          display: flex; align-items: center; gap: 4px;
          padding: 5px 8px 5px 12px;
          cursor: default; min-height: 30px;
          transition: background var(--t-fast);
          position: relative;
        }
        .tree-row:hover { background: var(--bg-2); }
        .tree-row--open > .tree-node-name { color: var(--text-0); }

        .tree-chevron {
          width: 18px; height: 18px; flex-shrink: 0;
          background: none; border: none; cursor: pointer;
          color: var(--text-3); display: flex; align-items: center; justify-content: center;
          border-radius: 4px; transition: color var(--t-fast), background var(--t-fast);
        }
        .tree-chevron:hover { color: var(--accent); background: var(--accent-light); }

        .tree-folder-icon { flex-shrink: 0; display: flex; align-items: center; color: var(--text-2); }

        .tree-node-name {
          flex: 1; font-size: 0.82rem; font-weight: 500;
          color: var(--text-1); cursor: pointer; overflow: hidden;
          text-overflow: ellipsis; white-space: nowrap;
          transition: color var(--t-fast);
        }
        .tree-node-name:hover { color: var(--text-0); }

        /* Akcje hover */
        .tree-actions {
          display: flex; gap: 3px; flex-shrink: 0;
        }
        .tree-action-btn {
          display: flex; align-items: center; gap: 2px;
          padding: 3px 6px; border-radius: 5px; border: 1px solid var(--border-1);
          background: #fff; color: var(--text-2); font-size: 0.65rem; font-weight: 600;
          cursor: pointer; white-space: nowrap;
          transition: all var(--t-fast);
        }
        .tree-action-btn:hover { background: var(--accent-light); color: var(--accent); border-color: var(--border-0); }
        .tree-action-btn--topic:hover { background: #f0fdf4; color: #16a34a; border-color: rgba(22,163,74,.25); }
        .tree-action-icon { display: flex; align-items: center; }
        .tree-action-text { font-size: 0.62rem; }

        /* Temat (liść) */
        .tree-topic {
          display: flex; align-items: center; gap: 7px;
          width: 100%; padding: 5px 10px 5px 12px;
          background: none; border: none; text-align: left;
          cursor: pointer; min-height: 28px;
          transition: background var(--t-fast);
        }
        .tree-topic:hover { background: var(--bg-2); }
        .tree-topic--active { background: var(--accent-light) !important; }
        .tree-topic--active .tree-topic-name { color: var(--accent); font-weight: 600; }
        .tree-topic-icon { color: var(--text-3); flex-shrink: 0; display: flex; }
        .tree-topic-name {
          flex: 1; font-size: 0.8rem; color: var(--text-1); overflow: hidden;
          text-overflow: ellipsis; white-space: nowrap;
        }
        .tree-topic-dot { font-size: 0.55rem; flex-shrink: 0; }

        /* Stany */
        .tree-loading {
          display: flex; align-items: center; gap: 6px;
          font-size: 0.75rem; color: var(--text-3); padding: 6px 12px;
        }
        .tree-empty { font-size: 0.73rem; color: var(--text-3); padding: 4px 12px; font-style: italic; }
        .tree-spinner {
          width: 10px; height: 10px; border-radius: 50%;
          border: 1.5px solid var(--border-1); border-top-color: var(--accent);
          animation: spin .6s linear infinite; flex-shrink: 0;
        }
        .tree-loading-full {
          display: flex; align-items: center; gap: 8px;
          padding: 24px 16px; font-size: 0.8rem; color: var(--text-2);
        }
        .tree-empty-full {
          padding: 32px 16px; text-align: center;
          font-size: 0.8rem; color: var(--text-3);
        }
        .tree-create-first {
          display: inline-block; margin-top: 8px;
          font-size: 0.78rem; color: var(--accent); font-weight: 600;
          background: none; border: none; cursor: pointer;
          transition: opacity .15s;
        }
        .tree-create-first:hover { opacity: .75; }
      `})]})}function _({topicTitle:t,onClose:n}){return e.jsxs("div",{className:"modal-backdrop",onClick:n,children:[e.jsxs("div",{className:"modal-box",onClick:r=>r.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("div",{children:[e.jsx("p",{className:"modal-eyebrow",children:"AI · Dopytaj o temat"}),e.jsx("h2",{className:"modal-title",children:t})]}),e.jsx("button",{className:"modal-close",onClick:n,children:"✕"})]}),e.jsxs("div",{className:"modal-body",children:[e.jsx("p",{className:"modal-info",children:"Zadaj dodatkowe pytanie dotyczące tej notatki. AI uzupełni lub rozszerzy treść na podstawie Twojego pytania."}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Twoje pytanie"}),e.jsx("textarea",{className:"mf-input mf-textarea",placeholder:"np. Wyjaśnij prościej, dodaj przykład z życia codziennego, pokaż jak to działa w Spring Boot...",rows:4,disabled:!0})]}),e.jsxs("div",{className:"modal-placeholder",children:[e.jsx("span",{className:"modal-placeholder-icon",children:"🔧"}),e.jsx("span",{children:"Integracja AI w przygotowaniu"})]})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{className:"btn-ghost",onClick:n,children:"Zamknij"}),e.jsx("button",{className:"btn-primary",disabled:!0,title:"Wkrótce dostępne",children:"Wyślij pytanie →"})]})]}),e.jsx("style",{children:`
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
      `})]})}const M=[{value:"BASIC",label:"Podstawowy",color:"#16a34a",bg:"#f0fdf4",border:"rgba(22,163,74,.25)"},{value:"MEDIUM",label:"Średni",color:"#d97706",bg:"#fffbeb",border:"rgba(217,119,6,.25)"},{value:"HARD",label:"Zaawansowany",color:"#dc2626",bg:"#fef2f2",border:"rgba(220,38,38,.25)"}];function q({topic:t,categoryPath:n,onTopicUpdated:r}){const[l,c]=i.useState(null),[u,x]=i.useState(!1),[m,f]=i.useState(t?.difficulty??"BASIC"),[b,j]=i.useState(!1),[p,h]=i.useState(!1),[g,v]=i.useState(!1);i.useEffect(()=>{if(!t){c(null);return}f(t.difficulty),x(!0),R.getByTopic(t.id).then(a=>c(a.data)).catch(a=>{a.status===404?c(null):N.error("Błąd ładowania notatki")}).finally(()=>x(!1))},[t?.id]);const k=async a=>{if(!(a===m||p)){f(a),h(!0);try{const s=await S.update(t.id,{difficulty:a});r?.(s.data)}catch{N.error("Błąd zapisu poziomu trudności"),f(t.difficulty)}finally{h(!1)}}};return t?(M.find(a=>a.value===m),e.jsxs("div",{className:"note-panel",children:[e.jsxs("div",{className:"note-header",children:[e.jsx("div",{className:"note-breadcrumb",children:n?.map((a,s)=>e.jsxs("span",{children:[s>0&&e.jsx("span",{className:"note-bc-sep",children:"›"}),e.jsx("span",{className:"note-bc-item",children:a.name})]},a.id))}),e.jsx("h1",{className:"note-title",children:t.title}),e.jsx("div",{className:"note-meta",children:e.jsx("span",{className:"note-status-badge","data-status":t.status,children:G[t.status]??t.status})})]}),e.jsxs("div",{className:"note-config",children:[e.jsxs("div",{className:"note-config-row",children:[e.jsx("span",{className:"note-config-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:M.map(a=>e.jsxs("button",{className:`diff-btn${m===a.value?" diff-btn--on":""}`,style:m===a.value?{background:a.bg,borderColor:a.border,color:a.color}:{},onClick:()=>k(a.value),disabled:p,children:[a.label,m===a.value&&p&&e.jsx("span",{className:"diff-saving"})]},a.value))})]}),e.jsx("div",{className:"note-config-row",children:e.jsxs("label",{className:"code-check",children:[e.jsx("input",{type:"checkbox",checked:b,onChange:a=>j(a.target.checked),className:"code-check-input"}),e.jsx("span",{className:"code-check-box",children:b&&e.jsx("svg",{width:"10",height:"10",viewBox:"0 0 10 10",fill:"none",children:e.jsx("path",{d:"M1.5 5.5L4 8l4.5-5.5",stroke:"white",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})})}),e.jsxs("span",{className:"code-check-label",children:[e.jsx("span",{className:"code-check-text",children:"Czy to KOD?"}),e.jsx("span",{className:"code-check-sub",children:"Zaznacz jeśli notatka dotyczy kodu / programowania"})]}),b&&e.jsxs("span",{className:"code-check-badge",children:[e.jsx("svg",{width:"11",height:"11",viewBox:"0 0 11 11",fill:"none",children:e.jsx("path",{d:"M7 2l2.5 3.5L7 9M4 2L1.5 5.5 4 9",stroke:"currentColor",strokeWidth:"1.4",strokeLinecap:"round",strokeLinejoin:"round"})}),"KOD"]})]})})]}),e.jsx("div",{className:"note-content-area",children:u?e.jsxs("div",{className:"note-loading",children:[e.jsx("span",{className:"note-spinner"}),e.jsx("span",{children:"Ładowanie notatki..."})]}):l?e.jsx(Y,{note:l}):e.jsxs("div",{className:"note-content-empty",children:[e.jsx("div",{className:"nce-icon",children:e.jsxs("svg",{width:"40",height:"40",viewBox:"0 0 40 40",fill:"none",children:[e.jsx("circle",{cx:"20",cy:"20",r:"17",stroke:"var(--border-1)",strokeWidth:"1.5"}),e.jsx("path",{d:"M13 20h14M20 13v14",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]})}),e.jsx("p",{className:"nce-title",children:"Brak notatki"}),e.jsxs("p",{className:"nce-sub",children:["Treść notatki pojawi się tutaj po wygenerowaniu przez AI.",e.jsx("br",{}),"Integracja z AI w przygotowaniu."]}),e.jsxs("div",{className:"nce-placeholder",children:[e.jsx("span",{className:"nce-placeholder-line",style:{width:"90%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"75%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"82%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"60%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"88%"}}),e.jsx("span",{className:"nce-placeholder-line",style:{width:"70%"}})]})]})}),e.jsxs("div",{className:"note-actions",children:[b&&e.jsxs("button",{className:"act-btn act-btn--code",title:"Wygeneruj zadania programistyczne",disabled:!0,children:[e.jsx("svg",{width:"15",height:"15",viewBox:"0 0 15 15",fill:"none",children:e.jsx("path",{d:"M10 3l3.5 4.5L10 12M5 3L1.5 7.5 5 12",stroke:"currentColor",strokeWidth:"1.6",strokeLinecap:"round",strokeLinejoin:"round"})}),"Wygeneruj zadania"]}),e.jsxs("div",{className:"act-right",children:[e.jsxs("button",{className:"act-btn act-btn--ask",onClick:()=>v(!0),children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("circle",{cx:"7",cy:"7",r:"6",stroke:"currentColor",strokeWidth:"1.4"}),e.jsx("path",{d:"M5.5 5.5C5.5 4.67 6.17 4 7 4s1.5.67 1.5 1.5c0 .8-.8 1.3-1.5 1.5v1",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"}),e.jsx("circle",{cx:"7",cy:"10.5",r:"0.6",fill:"currentColor"})]}),"Dopytaj o..."]}),e.jsxs("button",{className:"act-btn act-btn--quiz",disabled:!0,title:"Wkrótce dostępne",children:[e.jsxs("svg",{width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",children:[e.jsx("path",{d:"M2 2h10v10H2z",stroke:"currentColor",strokeWidth:"1.4",strokeLinejoin:"round"}),e.jsx("path",{d:"M5 5h4M5 7h4M5 9h2",stroke:"currentColor",strokeWidth:"1.3",strokeLinecap:"round"})]}),"Rozpocznij quiz"]})]})]}),g&&e.jsx(_,{topicTitle:t.title,onClose:()=>v(!1)}),e.jsx(L,{})]})):e.jsxs("div",{className:"note-empty-screen",children:[e.jsxs("div",{className:"note-empty-inner",children:[e.jsxs("svg",{width:"48",height:"48",viewBox:"0 0 48 48",fill:"none",children:[e.jsx("rect",{x:"6",y:"4",width:"36",height:"40",rx:"4",stroke:"var(--border-1)",strokeWidth:"2"}),e.jsx("path",{d:"M14 16h20M14 22h20M14 28h14",stroke:"var(--border-1)",strokeWidth:"2",strokeLinecap:"round"})]}),e.jsx("h2",{className:"note-empty-title",children:"Wybierz temat"}),e.jsxs("p",{className:"note-empty-sub",children:["Kliknij temat z drzewa kategorii po lewej,",e.jsx("br",{}),"aby zobaczyć lub stworzyć notatkę."]})]}),e.jsx(L,{})]})}function Y({note:t}){const n=t.content;return n?e.jsxs("article",{className:"note-article",children:[n.title&&e.jsx("h2",{className:"na-title",children:n.title}),n.summary&&e.jsx("p",{className:"na-summary",children:n.summary}),n.simpleExplanation&&e.jsxs("div",{className:"na-simple",children:[e.jsx("span",{className:"na-simple-label",children:"Jak dla dziecka"}),e.jsx("p",{children:n.simpleExplanation})]}),(n.sections??[]).map((r,l)=>e.jsxs("section",{className:"na-section",children:[e.jsx("h3",{className:"na-section-h",children:r.heading}),e.jsx("p",{className:"na-section-p",children:r.content})]},l)),(n.examples??[]).length>0&&e.jsxs("div",{className:"na-examples",children:[e.jsx("h3",{className:"na-sub-h",children:"Przykłady"}),n.examples.map((r,l)=>e.jsxs("div",{className:"na-example",children:[r.title&&e.jsx("p",{className:"na-example-title",children:r.title}),r.code&&e.jsx("pre",{className:"na-code",children:e.jsx("code",{children:r.code})}),r.explanation&&e.jsx("p",{className:"na-example-exp",children:r.explanation})]},l))]}),(n.memoryPoints??[]).length>0&&e.jsxs("div",{className:"na-list-section",children:[e.jsx("h3",{className:"na-sub-h",children:"Do zapamiętania"}),e.jsx("ul",{className:"na-list na-list--green",children:n.memoryPoints.map((r,l)=>e.jsx("li",{children:r},l))})]}),(n.commonMistakes??[]).length>0&&e.jsxs("div",{className:"na-list-section",children:[e.jsx("h3",{className:"na-sub-h",children:"Częste błędy"}),e.jsx("ul",{className:"na-list na-list--red",children:n.commonMistakes.map((r,l)=>e.jsx("li",{children:r},l))})]})]}):null}const G={NEW:"Nowy",NOTE_GENERATED:"Notatka gotowa",QUIZ_READY:"Quiz dostępny",PASSED:"Zaliczony",MASTERED:"Opanowany"};function L(){return e.jsx("style",{children:`
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
    `})}function J({parentId:t,parentName:n,onClose:r,onCreated:l}){const[c,u]=i.useState(""),[x,m]=i.useState(""),[f,b]=i.useState(!1),[j,p]=i.useState(""),h=async g=>{if(g.preventDefault(),!c.trim()){p("Nazwa jest wymagana");return}b(!0),p("");try{const v=await I.create({name:c.trim(),description:x.trim()||null,parentId:t??null});N.success(`Kategoria "${v.data.name}" utworzona`),l(v.data),r()}catch(v){p(v.message??"Błąd tworzenia kategorii")}finally{b(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:r,children:[e.jsxs("div",{className:"modal-box",onClick:g=>g.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsx("h2",{className:"modal-title",children:t?`Podkategoria w "${n}"`:"Nowa kategoria"}),e.jsx("button",{className:"modal-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:h,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Nazwa *"}),e.jsx("input",{className:"mf-input",value:c,onChange:g=>{u(g.target.value),p("")},placeholder:"np. Programowanie obiektowe",autoFocus:!0}),j&&e.jsx("span",{className:"mf-error",children:j})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Opis (opcjonalny)"}),e.jsx("input",{className:"mf-input",value:x,onChange:g=>m(g.target.value),placeholder:"Krótki opis kategorii"})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:f,children:f?e.jsx("span",{className:"btn-spin"}):"Utwórz"})]})]})]}),e.jsx(Q,{})]})}function Q(){return e.jsx("style",{children:`
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
    `})}const V=[{value:"BASIC",label:"Podstawowy",color:"#16a34a"},{value:"MEDIUM",label:"Średni",color:"#d97706"},{value:"HARD",label:"Zaawansowany",color:"#dc2626"}];function X({categoryId:t,categoryName:n,onClose:r,onCreated:l}){const[c,u]=i.useState(""),[x,m]=i.useState(""),[f,b]=i.useState("BASIC"),[j,p]=i.useState(!1),[h,g]=i.useState({}),v=()=>{const a={};return c.trim()||(a.title="Tytuł jest wymagany"),x.trim()||(a.shortPrompt="Kontekst jest wymagany"),a},k=async a=>{a.preventDefault();const s=v();if(Object.keys(s).length){g(s);return}p(!0);try{const w=await S.create({categoryId:t,title:c.trim(),shortPrompt:x.trim(),difficulty:f});N.success(`Temat "${w.data.title}" utworzony`),l(w.data),r()}catch(w){N.error(w.message??"Błąd tworzenia tematu")}finally{p(!1)}};return e.jsxs("div",{className:"modal-backdrop",onClick:r,children:[e.jsxs("div",{className:"modal-box modal-box--md",onClick:a=>a.stopPropagation(),children:[e.jsxs("header",{className:"modal-head",children:[e.jsxs("h2",{className:"modal-title",children:['Nowy temat w "',n,'"']}),e.jsx("button",{className:"modal-close",onClick:r,children:"✕"})]}),e.jsxs("form",{onSubmit:k,className:"modal-body",children:[e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Tytuł tematu *"}),e.jsx("input",{className:`mf-input${h.title?" mf-input--err":""}`,value:c,onChange:a=>{u(a.target.value),g(s=>({...s,title:""}))},placeholder:"np. Polimorfizm w Javie",autoFocus:!0}),h.title&&e.jsx("span",{className:"mf-error",children:h.title})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Kontekst dla AI *"}),e.jsx("textarea",{className:`mf-input mf-textarea${h.shortPrompt?" mf-input--err":""}`,value:x,onChange:a=>{m(a.target.value),g(s=>({...s,shortPrompt:""}))},placeholder:"Krótki opis co chcesz wiedzieć o tym temacie, np. wytłumacz z przykładami kodu, skupiony na praktycznym użyciu...",rows:3}),h.shortPrompt&&e.jsx("span",{className:"mf-error",children:h.shortPrompt})]}),e.jsxs("div",{className:"mf-field",children:[e.jsx("label",{className:"mf-label",children:"Poziom trudności"}),e.jsx("div",{className:"diff-group",children:V.map(a=>e.jsxs("label",{className:`diff-opt${f===a.value?" diff-opt--on":""}`,style:f===a.value?{borderColor:a.color,background:a.color+"12",color:a.color}:{},children:[e.jsx("input",{type:"radio",name:"difficulty",value:a.value,checked:f===a.value,onChange:()=>b(a.value),style:{display:"none"}}),a.label]},a.value))})]}),e.jsxs("footer",{className:"modal-foot",children:[e.jsx("button",{type:"button",className:"btn-ghost",onClick:r,children:"Anuluj"}),e.jsx("button",{type:"submit",className:"btn-primary",disabled:j,children:j?e.jsx("span",{className:"btn-spin"}):"Utwórz temat"})]})]})]}),e.jsx("style",{children:`
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
      `})]})}function re(){W("Dashboard");const[t,n]=i.useState([]),[r,l]=i.useState(!0),[c,u]=i.useState(new Set),[x,m]=i.useState({}),[f,b]=i.useState(new Set),[j,p]=i.useState(null),[h,g]=i.useState([]),[v,k]=i.useState(null),[a,s]=i.useState(null),w=i.useCallback(async()=>{l(!0);try{const o=await I.getTree();n(o.data.children??[])}catch(o){o.status!==401&&N.error("Błąd ładowania kategorii")}finally{l(!1)}},[]);i.useEffect(()=>{w()},[w]);const T=i.useCallback(async o=>{if(u(d=>{const y=new Set(d);return y.has(o)?(y.delete(o),y):(y.add(o),y)}),!x[o]){b(d=>new Set(d).add(o));try{const d=await S.listByCategory(o);m(y=>({...y,[o]:d.data}))}catch{}finally{b(d=>{const y=new Set(d);return y.delete(o),y})}}},[x]),P=i.useCallback(o=>{p(o),g(C(t,o.categoryId))},[t]),A=i.useCallback(o=>{w(),o.parentId&&u(d=>new Set(d).add(o.parentId))},[w]),E=i.useCallback(o=>{m(d=>({...d,[o.categoryId]:[...d[o.categoryId]??[],o]})),u(d=>new Set(d).add(o.categoryId)),p(o),g(C(t,o.categoryId))},[t]),B=i.useCallback(o=>{m(d=>({...d,[o.categoryId]:(d[o.categoryId]??[]).map(y=>y.id===o.id?o:y)})),p(d=>d?.id===o.id?o:d)},[]);return e.jsxs("div",{className:"db-root",children:[e.jsx(Z,{tree:t,loading:r,expanded:c,topics:x,loadingTopics:f,selectedTopicId:j?.id,onToggle:T,onSelectTopic:P,onAddRootCategory:()=>k({parentId:null,parentName:null}),onAddSubcategory:(o,d)=>k({parentId:o,parentName:d}),onAddTopic:(o,d)=>s({categoryId:o,categoryName:d})}),e.jsx(q,{topic:j,categoryPath:h,onTopicUpdated:B}),v!==null&&e.jsx(J,{parentId:v.parentId,parentName:v.parentName,onClose:()=>k(null),onCreated:A}),a!==null&&e.jsx(X,{categoryId:a.categoryId,categoryName:a.categoryName,onClose:()=>s(null),onCreated:E}),e.jsx("style",{children:`
        .db-root {
          display: flex;
          height: calc(100vh - 56px); /* topbar height */
          margin: -28px -32px;        /* negate main-content padding */
          overflow: hidden;
        }
      `})]})}function C(t,n,r=[]){for(const l of t){const c=[...r,{id:l.id,name:l.name}];if(l.id===n)return c;if(l.children?.length){const u=C(l.children,n,c);if(u)return u}}return[]}export{re as default};
