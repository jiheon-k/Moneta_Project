export function BrandPanel() {
  return <aside className="brand-panel" aria-label="Moneta 소개">
    <a className="brand" href="/" aria-label="Moneta 홈">
      <svg width="35" height="35" viewBox="0 0 40 40" fill="none" aria-hidden="true"><rect width="40" height="40" rx="12" fill="currentColor" /><path d="M10 27V13l10 9 10-9v14" stroke="#f7f7ef" strokeWidth="2.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
      <span>moneta<span className="brand-period">.</span></span>
    </a>
    <div className="brand-story">
      <p className="eyebrow"><span className="tiny-line" /> YOUR MONEY, YOUR RHYTHM</p>
      <h1>나의 돈을,<br />나의 리듬으로<span className="headline-period">.</span></h1>
      <p className="brand-description">복잡했던 돈 관리에 작은 여유를.<br />오늘의 기록으로 나다운 내일을 만들어 가세요.</p>
      <div className="ledger-scene" aria-hidden="true">
        <div className="scene-orbit orbit-one" /><div className="scene-orbit orbit-two" />
        <div className="paper paper-back" />
        <div className="paper paper-front">
          <div className="paper-top"><span className="paper-logo">m.</span><span>MY LITTLE MONEY NOTE</span><span className="paper-star">✳</span></div>
          <p className="paper-title">조금씩, 차곡차곡.</p>
          <p className="paper-caption">작은 기록이 쌓이는 나만의 공간</p>
          <div className="note-line"><span className="note-dot" /><i /><b /></div>
          <div className="note-line"><span className="note-dot yellow" /><i /><b /></div>
          <div className="note-line"><span className="note-dot" /><i /><b /></div>
          <div className="paper-bottom"><span>ONE DAY AT A TIME</span><span>↗</span></div>
        </div>
        <div className="coin"><span>₩</span></div>
        <div className="small-star">✳</div>
        <div className="scene-caption"><span className="caption-dot" />작은 습관, 가벼운 일상</div>
      </div>
    </div>
    <div className="brand-footer"><span>LESS WORRY. MORE LIVING.</span><span>© {new Date().getFullYear()} MONETA</span></div>
  </aside>
}
