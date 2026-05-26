import React, { useRef, useState } from 'react';
import { IonIcon } from '@ionic/react';
import { calendarOutline, checkmarkOutline, closeOutline } from 'ionicons/icons';

/* ---------------------------------------------------------------
   Helpers
--------------------------------------------------------------- */
const toISO = (d: Date) => d.toISOString().slice(0, 10); // YYYY-MM-DD

const todayISO = () => toISO(new Date());

const daysAgo = (n: number) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toISO(d);
};

/** ISO YYYY-MM-DD → hiển thị DD/MM/YYYY */
export const fmtDate = (iso: string) => {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

/** DD/MM/YYYY → ISO YYYY-MM-DD (trả về '' nếu không hợp lệ) */
const parseDisplay = (s: string): string => {
  const clean = s.replace(/[^\d]/g, '');
  if (clean.length < 8) return '';
  const d = clean.slice(0, 2);
  const m = clean.slice(2, 4);
  const y = clean.slice(4, 8);
  const iso = `${y}-${m}-${d}`;
  const dt = new Date(iso);
  if (isNaN(dt.getTime())) return '';
  // check actual values match (guard against e.g. Feb 31 rolling over)
  if (
    dt.getFullYear() !== Number(y) ||
    dt.getMonth() + 1 !== Number(m) ||
    dt.getDate() !== Number(d)
  ) return '';
  return iso;
};

/** Auto-insert slashes as user types: 2 → 2/ → 26/0 → 26/05/ → … */
const autoFormat = (raw: string): string => {
  const digits = raw.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
};

/** Build a human-friendly range label */
export const rangeLabelOf = (from: string, to: string): string => {
  if (!from || !to) return 'Tùy chọn';
  if (from === to) return fmtDate(from);
  return `${fmtDate(from)} – ${fmtDate(to)}`;
};

/* ---------------------------------------------------------------
   Quick-preset buttons
--------------------------------------------------------------- */
const PRESETS = [
  { label: 'Hôm nay',     from: () => todayISO(),  to: () => todayISO() },
  { label: 'Hôm qua',     from: () => daysAgo(1),  to: () => daysAgo(1) },
  { label: '7 ngày',      from: () => daysAgo(6),  to: () => todayISO() },
  { label: '30 ngày',     from: () => daysAgo(29), to: () => todayISO() },
  { label: 'Tháng này',
    from: () => { const d = new Date(); d.setDate(1); return toISO(d); },
    to:   () => todayISO() },
  { label: 'Tháng trước',
    from: () => { const d = new Date(); d.setDate(1); d.setMonth(d.getMonth() - 1); return toISO(d); },
    to:   () => { const d = new Date(); d.setDate(0); return toISO(d); } },
];

/* ---------------------------------------------------------------
   Props & Types
--------------------------------------------------------------- */
export interface DateRange {
  from: string; // YYYY-MM-DD
  to:   string; // YYYY-MM-DD
}

interface Props {
  isOpen: boolean;
  initialRange?: DateRange;
  onConfirm: (range: DateRange) => void;
  onClose: () => void;
}

/* ---------------------------------------------------------------
   DateInput – ô nhập có auto-format + native calendar fallback
--------------------------------------------------------------- */
interface DateInputProps {
  id: string;
  label: string;
  value: string;         // ISO YYYY-MM-DD
  onChange: (iso: string, displayVal: string) => void;
  min?: string;
  max?: string;
  error?: boolean;
}

const DateInput: React.FC<DateInputProps> = ({ id, label, value, onChange, min, max, error }) => {
  const [display, setDisplay] = useState(fmtDate(value));
  const [focused, setFocused] = useState(false);
  const nativeRef = useRef<HTMLInputElement>(null);

  // Sync display when external value changes (e.g. preset click)
  const prevValue = useRef(value);
  if (prevValue.current !== value) {
    prevValue.current = value;
    setDisplay(fmtDate(value));
  }

  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = autoFormat(e.target.value);
    setDisplay(formatted);
    const iso = parseDisplay(formatted);
    onChange(iso, formatted);
  };

  const handleNativeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const iso = e.target.value; // YYYY-MM-DD
    setDisplay(fmtDate(iso));
    onChange(iso, fmtDate(iso));
  };

  return (
    <div className="drp-input-group">
      <label className="drp-label" htmlFor={id}>
        <IonIcon icon={calendarOutline} />
        {label}
      </label>
      <div className={`drp-input-box ${focused ? 'focused' : ''} ${error ? 'error' : ''}`}>
        {/* Visible text input */}
        <input
          id={id}
          className="drp-text-input"
          type="text"
          inputMode="numeric"
          placeholder="DD/MM/YYYY"
          value={display}
          onChange={handleTextChange}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          maxLength={10}
          autoComplete="off"
        />
        {/* Calendar icon → opens native date picker */}
        <button
          type="button"
          className="drp-cal-icon-btn"
          aria-label="Chọn từ lịch"
          onClick={() => nativeRef.current?.showPicker?.()}
        >
          <IonIcon icon={calendarOutline} />
        </button>
        {/* Hidden native date input – opened programmatically */}
        <input
          ref={nativeRef}
          type="date"
          className="drp-native-hidden"
          value={value}
          min={min}
          max={max}
          onChange={handleNativeChange}
          tabIndex={-1}
          aria-hidden="true"
        />
      </div>
    </div>
  );
};

/* ---------------------------------------------------------------
   Main component
--------------------------------------------------------------- */
const DateRangePickerModal: React.FC<Props> = ({
  isOpen,
  initialRange,
  onConfirm,
  onClose,
}) => {
  const [from, setFrom] = useState(initialRange?.from ?? daysAgo(6));
  const [to,   setTo]   = useState(initialRange?.to   ?? todayISO());
  // raw display values (to detect partial input)
  const [fromDisplay, setFromDisplay] = useState(fmtDate(initialRange?.from ?? daysAgo(6)));
  const [toDisplay,   setToDisplay]   = useState(fmtDate(initialRange?.to   ?? todayISO()));
  const [error, setError] = useState('');
  const overlayRef = useRef<HTMLDivElement>(null);

  const applyPreset = (p: typeof PRESETS[0]) => {
    const f = p.from();
    const t = p.to();
    setFrom(f); setFromDisplay(fmtDate(f));
    setTo(t);   setToDisplay(fmtDate(t));
    setError('');
  };

  const validate = (): string => {
    if (!from || !to) return 'Vui lòng nhập đủ ngày bắt đầu và kết thúc (DD/MM/YYYY)';
    if (from > to)    return 'Ngày bắt đầu phải trước ngày kết thúc';
    const diff = (new Date(to).getTime() - new Date(from).getTime()) / 86_400_000;
    if (diff > 365)   return 'Khoảng thời gian tối đa là 365 ngày';
    return '';
  };

  const handleConfirm = () => {
    const err = validate();
    if (err) { setError(err); return; }
    onConfirm({ from, to });
  };

  const diffDays = from && to && from <= to
    ? Math.round((new Date(to).getTime() - new Date(from).getTime()) / 86_400_000) + 1
    : null;

  if (!isOpen) return null;

  return (
    <div
      ref={overlayRef}
      className="drp-backdrop"
      onClick={(e) => { if (e.target === overlayRef.current) onClose(); }}
    >
      <div className="drp-sheet" role="dialog" aria-modal="true" aria-label="Chọn khoảng ngày">

        {/* Handle */}
        <div className="drp-handle" />

        {/* Header */}
        <div className="drp-header">
          <button className="drp-close-btn" onClick={onClose} aria-label="Đóng">
            <IonIcon icon={closeOutline} />
          </button>
          <span className="drp-title">Chọn khoảng ngày</span>
          <button className="drp-confirm-btn" onClick={handleConfirm}>
            <IonIcon icon={checkmarkOutline} />
            Áp dụng
          </button>
        </div>

        {/* Presets */}
        <div className="drp-presets">
          {PRESETS.map((p) => {
            const pf = p.from(); const pt = p.to();
            return (
              <button
                key={p.label}
                className={`drp-preset-chip ${from === pf && to === pt ? 'active' : ''}`}
                onClick={() => applyPreset(p)}
              >
                {p.label}
              </button>
            );
          })}
        </div>

        <div className="drp-divider" />

        {/* Date inputs */}
        <div className="drp-inputs">
          <DateInput
            id="drp-from"
            label="Từ ngày"
            value={from}
            onChange={(iso, disp) => { setFrom(iso); setFromDisplay(disp); setError(''); }}
            max={to || todayISO()}
          />
          <div className="drp-input-sep">→</div>
          <DateInput
            id="drp-to"
            label="Đến ngày"
            value={to}
            onChange={(iso, disp) => { setTo(iso); setToDisplay(disp); setError(''); }}
            min={from}
            max={todayISO()}
          />
        </div>

        {/* Range summary */}
        {diffDays && (
          <div className="drp-range-summary">
            <IonIcon icon={calendarOutline} />
            {rangeLabelOf(from, to)} &nbsp;·&nbsp; {diffDays} ngày
          </div>
        )}

        {/* Error */}
        {error && <div className="drp-error">{error}</div>}

        {/* Apply button */}
        <button className="drp-apply-btn" onClick={handleConfirm}>
          Xem báo cáo
        </button>
      </div>
    </div>
  );
};

export default DateRangePickerModal;
