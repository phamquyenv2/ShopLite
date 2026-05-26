import React, { useEffect, useState } from 'react';
import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonToolbar,
} from '@ionic/react';
import {
  calendarOutline,
  caretDown,
  chevronBackOutline,
  chevronForwardOutline,
  downloadOutline,
  printOutline,
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import DateRangePickerModal, { DateRange, rangeLabelOf } from '../components/DateRangePickerModal';
import './Reports.css';

/* ---------- helpers ---------- */
const fmt = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v)) + 'đ';

const fmtCount = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(v);

const todayLabel = () => {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
};

/* ---------- types ---------- */
type Period = 'today' | 'yesterday' | 'week' | 'month' | 'custom';

interface DaySummary {
  totalRevenue: number;
  totalOrders: number;
  totalProducts: number;
  totalDiscount: number;
  totalRefund: number;
  netRevenue: number;
  avgOrderValue: number;
  newCustomers: number;
  cashAmount: number;
  bankAmount: number;
  ewalletAmount: number;
}

interface TopProduct {
  name: string;
  qty: number;
  revenue: number;
}

/* ---------- mock data ---------- */
const mockSummary = (period: Period, customRange?: DateRange): DaySummary => {
  let multiplier = 1;
  if (period === 'yesterday') multiplier = 0.85;
  else if (period === 'week') multiplier = 6.3;
  else if (period === 'month') multiplier = 28.5;
  else if (period === 'custom' && customRange) {
    multiplier = Math.max(1, Math.round(
      (new Date(customRange.to).getTime() - new Date(customRange.from).getTime()) / 86_400_000
    ) + 1);
  }
  const base = 12_350_000;
  const orders = Math.round(14 * multiplier);
  const revenue = Math.round(base * multiplier);
  const discount = Math.round(revenue * 0.05);
  const refund = Math.round(revenue * 0.02);
  return {
    totalRevenue: revenue,
    totalOrders: orders,
    totalProducts: Math.round(38 * multiplier),
    totalDiscount: discount,
    totalRefund: refund,
    netRevenue: revenue - discount - refund,
    avgOrderValue: orders > 0 ? Math.round(revenue / orders) : 0,
    newCustomers: Math.round(3 * multiplier),
    cashAmount: Math.round(revenue * 0.45),
    bankAmount: Math.round(revenue * 0.38),
    ewalletAmount: Math.round(revenue * 0.17),
  };
};

const mockTopProducts = (): TopProduct[] => [
  { name: 'Cà phê sữa đá', qty: 42, revenue: 2_310_000 },
  { name: 'Bánh mì thịt', qty: 35, revenue: 1_750_000 },
  { name: 'Nước cam ép', qty: 28, revenue: 1_260_000 },
  { name: 'Trà đào cam sả', qty: 22, revenue: 1_100_000 },
  { name: 'Sandwich trứng', qty: 18, revenue: 810_000 },
];

const PERIODS: { key: Exclude<Period, 'custom'>; label: string }[] = [
  { key: 'today',     label: 'Hôm nay'   },
  { key: 'yesterday', label: 'Hôm qua'   },
  { key: 'week',      label: '7 ngày'    },
  { key: 'month',     label: 'Tháng này' },
];

const PAYMENT_ROWS = [
  { key: 'cash',   label: 'Tiền mặt',    emoji: '💵', cls: ''       },
  { key: 'bank',   label: 'Chuyển khoản',emoji: '🏦', cls: 'green'  },
  { key: 'ewallet',label: 'Ví điện tử',  emoji: '📱', cls: 'orange' },
] as const;

/* ============================================================ */
const ReportEndOfDayPage: React.FC = () => {
  const history = useHistory();
  const [period, setPeriod] = useState<Period>('today');
  const [customRange, setCustomRange] = useState<DateRange | null>(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<DaySummary>(mockSummary('today'));
  const [topProducts, setTopProducts] = useState<TopProduct[]>(mockTopProducts());

  const load = (p: Period = period, cr: DateRange | null = customRange) => {
    setLoading(true);
    setTimeout(() => {
      setSummary(mockSummary(p, cr ?? undefined));
      setTopProducts(mockTopProducts());
      setLoading(false);
    }, 500);
  };

  useEffect(() => {
    load(period);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const handleCustomConfirm = (range: DateRange) => {
    setCustomRange(range);
    setPeriod('custom');
    setPickerOpen(false);
    load('custom', range);
  };

  const periodLabel = (): string => {
    if (period === 'custom' && customRange) return rangeLabelOf(customRange.from, customRange.to);
    if (period === 'today')     return `Hôm nay, ${todayLabel()}`;
    if (period === 'yesterday') return 'Hôm qua';
    if (period === 'week')      return '7 ngày gần đây';
    return 'Tháng này';
  };

  const maxRevenue   = Math.max(...topProducts.map(p => p.revenue), 1);
  const paymentTotal = summary.cashAmount + summary.bankAmount + summary.ewalletAmount;
  const pct = (v: number) => paymentTotal > 0 ? Math.round((v / paymentTotal) * 100) : 0;

  const paymentValues: Record<string, number> = {
    cash:    summary.cashAmount,
    bank:    summary.bankAmount,
    ewallet: summary.ewalletAmount,
  };

  return (
    <IonPage className="rpt-page">
      <IonHeader className="rpt-header ion-no-border">
        <IonToolbar className="rpt-toolbar">
          <div className="rpt-toolbar-inner">
            <button className="rpt-back-btn" aria-label="Quay lại" onClick={() => history.goBack()}>
              <IonIcon icon={chevronBackOutline} />
            </button>
            <span className="rpt-title">Báo cáo cuối ngày</span>
            <div className="rpt-header-actions">
              <button className="rpt-icon-btn" aria-label="In báo cáo">
                <IonIcon icon={printOutline} />
              </button>
              <button className="rpt-icon-btn" aria-label="Xuất Excel">
                <IonIcon icon={downloadOutline} />
              </button>
            </div>
          </div>
        </IonToolbar>

        <div className="rpt-period-bar">
          <button
            className={`rpt-period-custom ${period === 'custom' ? 'active' : ''}`}
            onClick={() => setPickerOpen(true)}
          >
            <IonIcon icon={calendarOutline} />
            {period === 'custom' && customRange
              ? rangeLabelOf(customRange.from, customRange.to)
              : 'Tùy chọn'}
            <IonIcon icon={caretDown} />
          </button>
          {PERIODS.map(p => (
            <button
              key={p.key}
              className={`rpt-period-chip ${period === p.key ? 'active' : ''}`}
              onClick={() => { setPeriod(p.key); setCustomRange(null); }}
            >
              {p.label}
            </button>
          ))}
        </div>
      </IonHeader>

      <IonContent className="rpt-content">
        <IonRefresher slot="fixed" onIonRefresh={async e => { load(); e.detail.complete(); }}>
          <IonRefresherContent />
        </IonRefresher>

        {loading ? (
          <div className="rpt-loading"><IonSpinner name="crescent" /></div>
        ) : (
          <div className="rpt-body">

            {/* Date note */}
            <div className="rpt-date-note">
              <IonIcon icon={calendarOutline} />
              {periodLabel()}
            </div>

            {/* Summary cards */}
            <div className="rpt-summary-grid">
              <div className="rpt-summary-card accent-blue span-2">
                <div className="rpt-card-label">Doanh thu thuần</div>
                <div className="rpt-card-value">{fmt(summary.netRevenue)}</div>
                <div className="rpt-card-sub">
                  Tổng đơn: {fmtCount(summary.totalOrders)} • Đã thu: {fmt(summary.totalRevenue)}
                </div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Số đơn hàng</div>
                <div className="rpt-card-value neutral">{fmtCount(summary.totalOrders)}</div>
                <div className="rpt-card-sub">đơn</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Giá trị TB / đơn</div>
                <div className="rpt-card-value compact neutral">{fmt(summary.avgOrderValue)}</div>
                <div className="rpt-card-sub">/ đơn</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Giảm giá</div>
                <div className="rpt-card-value compact danger">{fmt(summary.totalDiscount)}</div>
                <div className="rpt-card-sub">chiết khấu</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Hoàn trả</div>
                <div className="rpt-card-value compact warning">{fmt(summary.totalRefund)}</div>
                <div className="rpt-card-sub">trả lại</div>
              </div>
            </div>

            {/* Payment breakdown */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Phương thức thanh toán</span>
                <span className="rpt-section-link">
                  Chi tiết <IonIcon icon={chevronForwardOutline} />
                </span>
              </div>

              <div className="rpt-chart-wrap">
                {PAYMENT_ROWS.map(row => (
                  <div key={row.key} className="rpt-bar-row">
                    <span className="rpt-bar-label">
                      {row.key === 'cash' ? 'Tiền' : row.key === 'bank' ? 'Bank' : 'Ví'}
                    </span>
                    <div className="rpt-bar-track">
                      <div
                        className={`rpt-bar-fill ${row.cls}`}
                        style={{ width: `${pct(paymentValues[row.key])}%` }}
                      />
                    </div>
                    <span className="rpt-bar-val">{pct(paymentValues[row.key])}%</span>
                  </div>
                ))}
              </div>

              <div className="rpt-metric-list">
                {PAYMENT_ROWS.map(row => (
                  <div key={row.key} className="rpt-metric-item">
                    <span className="rpt-metric-label">{row.emoji} {row.label}</span>
                    <span className="rpt-metric-value">{fmt(paymentValues[row.key])}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Top products */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Sản phẩm bán chạy</span>
                <button className="rpt-section-link">
                  Xem tất cả <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {topProducts.map((p, idx) => {
                const rankClass = idx === 0 ? 'gold' : idx === 1 ? 'silver' : idx === 2 ? 'bronze' : 'normal';
                return (
                  <div key={p.name} className="rpt-progress-row">
                    <div className={`rpt-rank ${rankClass}`}>{idx + 1}</div>
                    <div className="rpt-progress-info">
                      <div className="rpt-progress-name">{p.name}</div>
                      <div className="rpt-progress-track">
                        <div
                          className="rpt-progress-fill"
                          style={{ width: `${Math.round((p.revenue / maxRevenue) * 100)}%` }}
                        />
                      </div>
                    </div>
                    <div className="rpt-progress-stat">
                      <div className="val">{fmt(p.revenue)}</div>
                      <div className="sub">{fmtCount(p.qty)} sp</div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Additional metrics */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Chỉ số khác</span>
              </div>
              <div className="rpt-metric-list">
                <div className="rpt-metric-item">
                  <span className="rpt-metric-label">Sản phẩm bán ra</span>
                  <span className="rpt-metric-value blue">{fmtCount(summary.totalProducts)} sp</span>
                </div>
                <div className="rpt-metric-item">
                  <span className="rpt-metric-label">Khách hàng mới</span>
                  <span className="rpt-metric-value blue">{fmtCount(summary.newCustomers)} khách</span>
                </div>
                <div className="rpt-metric-item">
                  <span className="rpt-metric-label">Giảm giá đã áp dụng</span>
                  <span className="rpt-metric-value neg">{fmt(summary.totalDiscount)}</span>
                </div>
                <div className="rpt-metric-item">
                  <span className="rpt-metric-label">Đơn hoàn trả</span>
                  <span className="rpt-metric-value neg">{fmt(summary.totalRefund)}</span>
                </div>
              </div>
            </div>

            <div className="rpt-bottom-spacer" />
          </div>
        )}
      </IonContent>

      <DateRangePickerModal
        isOpen={pickerOpen}
        initialRange={customRange ?? undefined}
        onConfirm={handleCustomConfirm}
        onClose={() => setPickerOpen(false)}
      />
    </IonPage>
  );
};

export default ReportEndOfDayPage;
