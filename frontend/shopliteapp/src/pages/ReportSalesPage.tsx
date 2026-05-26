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
  trendingUpOutline,
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import DateRangePickerModal, { DateRange, rangeLabelOf } from '../components/DateRangePickerModal';
import './Reports.css';

/* ---------- helpers ---------- */
const fmt = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v)) + 'đ';

const fmtCount = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(v);

const fmtK = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v / 1_000)) + 'K';

/* ---------- types ---------- */
type Period = 'today' | 'week' | 'month' | 'quarter' | 'custom';

type RevenuePoint = { label: string; value: number };

interface SalesSummary {
  totalRevenue: number;
  totalOrders: number;
  totalDiscount: number;
  netRevenue: number;
  avgOrderValue: number;
  returnAmount: number;
  growth: number;
}

interface TopCategory {
  name: string;
  revenue: number;
  orders: number;
  pct: number;
}

interface RecentOrder {
  code: string;
  customer: string;
  amount: number;
  status: 'PAID' | 'PENDING' | 'CANCELLED';
  time: string;
}

/* ---------- mock data ---------- */
const REVENUE_POINTS: Record<Exclude<Period, 'custom'>, RevenuePoint[]> = {
  today: [
    { label: '08-10h', value: 450_000 },
    { label: '10-12h', value: 950_000 },
    { label: '12-14h', value: 2_100_000 },
    { label: '14-16h', value: 650_000 },
    { label: '16-18h', value: 800_000 },
    { label: '18-20h', value: 1_500_000 },
    { label: '20-22h', value: 450_000 },
  ],
  week: [
    { label: 'T2', value: 8_200_000 },
    { label: 'T3', value: 11_500_000 },
    { label: 'T4', value: 9_800_000 },
    { label: 'T5', value: 14_200_000 },
    { label: 'T6', value: 16_700_000 },
    { label: 'T7', value: 12_300_000 },
    { label: 'CN', value: 7_400_000 },
  ],
  month: Array.from({ length: 8 }, (_, i) => ({
    label: `T${i + 1}`,
    value: 8_000_000 + (i % 3) * 2_000_000,
  })),
  quarter: [
    { label: 'Tháng 3', value: 145_000_000 },
    { label: 'Tháng 4', value: 178_000_000 },
    { label: 'Tháng 5', value: 193_000_000 },
  ],
};

const mockSummary = (period: Period, customRange?: DateRange): SalesSummary => {
  let mult = 1;
  if (period === 'week')    mult = 7;
  else if (period === 'month')   mult = 30;
  else if (period === 'quarter') mult = 90;
  else if (period === 'custom' && customRange) {
    mult = Math.max(1, Math.round(
      (new Date(customRange.to).getTime() - new Date(customRange.from).getTime()) / 86_400_000
    ) + 1);
  }
  const rev  = 6_900_000 * mult;
  const disc = rev * 0.04;
  const ret  = rev * 0.015;
  return {
    totalRevenue:  rev,
    totalOrders:   14 * mult,
    totalDiscount: disc,
    netRevenue:    rev - disc - ret,
    avgOrderValue: 493_000,
    returnAmount:  ret,
    growth:        12.4,
  };
};

const mockCategories = (): TopCategory[] => [
  { name: 'Đồ uống',       revenue: 3_120_000, orders: 42, pct: 45 },
  { name: 'Thức ăn nhanh', revenue: 1_890_000, orders: 28, pct: 27 },
  { name: 'Bánh & Snack',  revenue:   980_000, orders: 18, pct: 14 },
  { name: 'Tráng miệng',   revenue:   620_000, orders: 12, pct:  9 },
  { name: 'Khác',          revenue:   290_000, orders:  6, pct:  5 },
];

const mockRecentOrders = (): RecentOrder[] => [
  { code: 'HD001234', customer: 'Nguyễn Văn A', amount: 285_000, status: 'PAID',      time: '18:42' },
  { code: 'HD001233', customer: 'Trần Thị B',   amount: 156_000, status: 'PAID',      time: '17:30' },
  { code: 'HD001232', customer: 'Lê Minh C',    amount: 540_000, status: 'PENDING',   time: '16:15' },
  { code: 'HD001231', customer: 'Phạm Thu D',   amount:  89_000, status: 'CANCELLED', time: '15:00' },
  { code: 'HD001230', customer: 'Hoàng Văn E',  amount: 320_000, status: 'PAID',      time: '14:22' },
];

const PERIODS: { key: Exclude<Period, 'custom'>; label: string }[] = [
  { key: 'today',   label: 'Hôm nay'  },
  { key: 'week',    label: '7 ngày'   },
  { key: 'month',   label: 'Tháng này'},
  { key: 'quarter', label: 'Quý này'  },
];

const STATUS_MAP: Record<RecentOrder['status'], { label: string; cls: string }> = {
  PAID:      { label: 'Đã thu',  cls: ''     },
  PENDING:   { label: 'Chờ TT',  cls: 'blue' },
  CANCELLED: { label: 'Đã huỷ', cls: 'red'  },
};

/* ============================================================ */
const ReportSalesPage: React.FC = () => {
  const history = useHistory();
  const [period, setPeriod]           = useState<Period>('today');
  const [customRange, setCustomRange] = useState<DateRange | null>(null);
  const [pickerOpen, setPickerOpen]   = useState(false);
  const [loading, setLoading]         = useState(false);
  const [summary, setSummary]         = useState<SalesSummary>(mockSummary('today'));
  const [categories, setCategories]   = useState<TopCategory[]>(mockCategories());
  const [recentOrders, setRecentOrders] = useState<RecentOrder[]>(mockRecentOrders());

  const load = (p: Period = period, cr: DateRange | null = customRange) => {
    setLoading(true);
    setTimeout(() => {
      setSummary(mockSummary(p, cr ?? undefined));
      setCategories(mockCategories());
      setRecentOrders(mockRecentOrders());
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

  const basePoints = period !== 'custom' ? REVENUE_POINTS[period] : REVENUE_POINTS['today'];
  const maxPoint   = Math.max(...basePoints.map(p => p.value), 1);

  return (
    <IonPage className="rpt-page">
      <IonHeader className="rpt-header ion-no-border">
        <IonToolbar className="rpt-toolbar">
          <div className="rpt-toolbar-inner">
            <button className="rpt-back-btn" aria-label="Quay lại" onClick={() => history.goBack()}>
              <IonIcon icon={chevronBackOutline} />
            </button>
            <span className="rpt-title">Báo cáo bán hàng</span>
            <div className="rpt-header-actions">
              <button className="rpt-icon-btn" aria-label="Xuất báo cáo">
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

            {/* Growth banner */}
            <div className="rpt-info-banner success">
              <IonIcon icon={trendingUpOutline} className="rpt-info-icon" />
              <div>
                <div className="rpt-info-title">
                  Tăng trưởng {summary.growth}% so với kỳ trước
                </div>
                <div className="rpt-info-sub">Doanh thu đang trên đà tăng tốt</div>
              </div>
            </div>

            {/* Summary cards */}
            <div className="rpt-summary-grid">
              <div className="rpt-summary-card accent-blue span-2">
                <div className="rpt-card-label">Doanh thu thuần</div>
                <div className="rpt-card-value">{fmt(summary.netRevenue)}</div>
                <div className="rpt-card-sub">
                  {fmtCount(summary.totalOrders)} đơn • Tổng thu: {fmt(summary.totalRevenue)}
                </div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Đơn hàng</div>
                <div className="rpt-card-value neutral">{fmtCount(summary.totalOrders)}</div>
                <div className="rpt-card-sub">đơn</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">TB / đơn</div>
                <div className="rpt-card-value compact neutral">{fmt(summary.avgOrderValue)}</div>
                <div className="rpt-card-sub">giá trị trung bình</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Giảm giá</div>
                <div className="rpt-card-value compact danger">{fmt(summary.totalDiscount)}</div>
                <div className="rpt-card-sub">chiết khấu</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Hoàn trả</div>
                <div className="rpt-card-value compact warning">{fmt(summary.returnAmount)}</div>
                <div className="rpt-card-sub">trả lại KH</div>
              </div>
            </div>

            {/* Revenue chart */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Biểu đồ doanh thu</span>
              </div>
              <div className="rpt-chart-wrap">
                {basePoints.map(pt => (
                  <div key={pt.label} className="rpt-bar-row">
                    <span className="rpt-bar-label">{pt.label}</span>
                    <div className="rpt-bar-track">
                      <div
                        className="rpt-bar-fill green"
                        style={{ width: `${Math.round((pt.value / maxPoint) * 100)}%` }}
                      />
                    </div>
                    <span className="rpt-bar-val">{fmtK(pt.value)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Top categories */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Danh mục bán chạy</span>
                <button className="rpt-section-link">
                  Xem tất cả <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {categories.map((cat, idx) => {
                const rankClass = idx === 0 ? 'gold' : idx === 1 ? 'silver' : idx === 2 ? 'bronze' : 'normal';
                return (
                  <div key={cat.name} className="rpt-progress-row">
                    <div className={`rpt-rank ${rankClass}`}>{idx + 1}</div>
                    <div className="rpt-progress-info">
                      <div className="rpt-progress-name">{cat.name}</div>
                      <div className="rpt-progress-track">
                        <div className="rpt-progress-fill green" style={{ width: `${cat.pct}%` }} />
                      </div>
                    </div>
                    <div className="rpt-progress-stat">
                      <div className="val">{fmt(cat.revenue)}</div>
                      <div className="sub">{fmtCount(cat.orders)} đơn</div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Recent orders */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Đơn hàng gần đây</span>
                <button className="rpt-section-link" onClick={() => history.push('/orders')}>
                  Xem tất cả <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {recentOrders.map(order => {
                const { label, cls } = STATUS_MAP[order.status];
                return (
                  <div
                    key={order.code}
                    className="rpt-row"
                    onClick={() => history.push('/orders')}
                    style={{ cursor: 'pointer' }}
                  >
                    <div className="rpt-row-left">
                      <div className="rpt-row-title">{order.code}</div>
                      <div className="rpt-row-sub">{order.customer} • {order.time}</div>
                    </div>
                    <div className="rpt-row-right">
                      <div className="rpt-row-amount pos">{fmt(order.amount)}</div>
                      <div className={`rpt-row-badge ${cls}`}>{label}</div>
                    </div>
                  </div>
                );
              })}
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

export default ReportSalesPage;
