import React, { useEffect, useState, useMemo } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent,
    IonIcon, IonSpinner, IonFab, IonFabButton
} from '@ionic/react';
import {
    chevronBackOutline, searchOutline, swapVerticalOutline,
    funnel, caretDown, addOutline
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { fundAccountService } from '../services/fundAccount.service';
import { transactionService } from '../services/transaction.service';
import type { FundAccount, Transaction } from '../api/types';
import { useStorePermissions } from '../utils/useStorePermissions';
import './FundLedgerPage.css';

/* ---------- helpers ---------- */
const fmt = (v: number) => v.toLocaleString('vi-VN');

const fmtDateTime = (s: string | null | undefined) => {
    if (!s) return '';
    const d = new Date(s);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const groupLabel = (s: string | null | undefined) => {
    if (!s) return 'KHÔNG RÕ';
    const d = new Date(s);
    const today = new Date();
    const yesterday = new Date(); yesterday.setDate(today.getDate() - 1);
    const pad = (n: number) => n.toString().padStart(2, '0');
    const label = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
    if (d.toDateString() === today.toDateString()) return `HÔM NAY ${label}`;
    if (d.toDateString() === yesterday.toDateString()) return `HÔM QUA ${label}`;
    return label;
};

/* ---------- Tab type ---------- */
type TabKey = 'CASH' | 'BANK' | 'EWALLET' | 'ALL';
const TABS: { key: TabKey; label: string }[] = [
    { key: 'CASH',   label: 'Tiền mặt' },
    { key: 'BANK',   label: 'Ngân hàng' },
    { key: 'EWALLET', label: 'Ví điện tử' },
    { key: 'ALL',    label: 'Tổng quỹ' },
];

/* ============================= */
const FundLedgerPage: React.FC = () => {
    const history = useHistory();
    const { can } = useStorePermissions();
    const canCreateTransaction = can('/api/v1/transactions', 'POST');
    const [activeTab, setActiveTab] = useState<TabKey>('CASH');
    const [fundAccounts, setFundAccounts] = useState<FundAccount[]>([]);
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    /* ---- Load fund accounts once ---- */
    useEffect(() => {
        fundAccountService.getFundAccounts()
            .then(accounts => setFundAccounts(accounts))
            .catch(e => console.error('Lỗi tải quỹ:', e));
    }, []);

    /* ---- Load transactions when tab changes ---- */
    useEffect(() => {
        setLoading(true);
        setError(null);
        const load = async () => {
            try {
                let txs: Transaction[] = [];
                if (activeTab === 'ALL') {
                    txs = await transactionService.getTransactions();
                } else {
                    // Find fund accounts matching this type
                    const matching = fundAccounts.filter(f => f.type === activeTab);
                    if (matching.length === 0) {
                        setTransactions([]);
                        return;
                    }
                    // Fetch all in parallel
                    const results = await Promise.all(
                        matching.map(f => transactionService.getTransactionsByFundAccount(f.id))
                    );
                    txs = results.flat();
                }
                // Sort newest first
                txs.sort((a, b) => {
                    const tA = a.transactionTime ? new Date(a.transactionTime).getTime() : 0;
                    const tB = b.transactionTime ? new Date(b.transactionTime).getTime() : 0;
                    return tB - tA;
                });
                setTransactions(txs);
            } catch (e: any) {
                setError(e?.message || 'Lỗi tải giao dịch');
            } finally {
                setLoading(false);
            }
        };
        // Only load if fund accounts ready (for non-ALL tabs)
        if (activeTab === 'ALL' || fundAccounts.length > 0) {
            load();
        }
    }, [activeTab, fundAccounts]);

    /* ---- Summary ---- */
    const summary = useMemo(() => {
        let totalIn = 0;
        let totalOut = 0;
        for (const tx of transactions) {
            if (tx.direction === 'IN') totalIn += tx.amount;
            else totalOut += tx.amount;
        }
        let balance = 0;
        let opening = 0;
        if (activeTab === 'ALL') {
            balance = fundAccounts.reduce((s, f) => s + f.balance, 0);
            opening = fundAccounts.reduce((s, f) => s + f.openingBalance, 0);
        } else {
            const matching = fundAccounts.filter(f => f.type === activeTab);
            balance = matching.reduce((s, f) => s + f.balance, 0);
            opening = matching.reduce((s, f) => s + f.openingBalance, 0);
        }
        return { opening, totalIn, totalOut, balance };
    }, [transactions, activeTab, fundAccounts]);

    /* ---- Group by date ---- */
    const groups = useMemo(() => {
        const map: Record<string, Transaction[]> = {};
        const keys: string[] = [];
        for (const tx of transactions) {
            const k = groupLabel(tx.transactionTime || tx.createdAt);
            if (!map[k]) { map[k] = []; keys.push(k); }
            map[k].push(tx);
        }
        return { map, keys };
    }, [transactions]);

    /* ---- Display helpers ---- */
    const txCode = (tx: Transaction) => tx.transactionCode || `TX${tx.id.toString().padStart(6, '0')}`;
    const txDesc = (tx: Transaction) => {
        if (tx.content) return tx.content;
        switch (tx.type) {
            case 'REVENUE':    return 'Tiền thu khách';
            case 'EXPENSE':    return 'Tiền trả NCC';
            case 'SALARY':     return 'Trả lương';
            case 'REFUND':     return 'Tiền hoàn trả';
            case 'ADJUSTMENT': return 'Chuyển/ Rút';
            default:           return 'Giao dịch khác';
        }
    };
    const txAmount = (tx: Transaction) => {
        const neg = tx.direction === 'OUT';
        return { value: `${neg ? '-' : ''}${fmt(tx.amount)}`, neg };
    };

    return (
        <IonPage className="flp">
            {/* ── Header ── */}
            <IonHeader className="ion-no-border flp-header">
                <IonToolbar className="flp-toolbar">
                    <div className="flp-toolbar-inner">
                        <button className="flp-back-btn" onClick={() => history.goBack()}>
                            <IonIcon icon={chevronBackOutline} />
                        </button>
                        <span className="flp-title">Sổ quỹ</span>
                        <div className="flp-toolbar-actions">
                            <button className="flp-icon-btn"><IonIcon icon={searchOutline} /></button>
                            <button className="flp-icon-btn"><IonIcon icon={swapVerticalOutline} /></button>
                        </div>
                    </div>
                </IonToolbar>

                {/* ── Tabs ── */}
                <div className="flp-tabs">
                    {TABS.map(t => (
                        <button
                            key={t.key}
                            className={`flp-tab ${activeTab === t.key ? 'active' : ''}`}
                            onClick={() => setActiveTab(t.key)}
                        >
                            {t.label}
                        </button>
                    ))}
                </div>
            </IonHeader>

            <IonContent className="flp-content">
                {/* ── Filter bar ── */}
                <div className="flp-filter-bar">
                    <div className="flp-filter-left">
                        <button className="ia-filter-btn" type="button" aria-label="Bộ lọc">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                                stroke="currentColor" strokeWidth="2"
                                strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6" />
                                <line x1="8" y1="12" x2="16" y2="12" />
                                <line x1="10" y1="18" x2="14" y2="18" />
                            </svg>
                        </button>
                        <button className="flp-month-btn flp-month-pill">
                            Tháng này <IonIcon icon={caretDown} />
                        </button>
                    </div>
                    <span className="flp-count">{transactions.length} phiếu</span>
                </div>

                {error && (
                    <div className="flp-error">{error}</div>
                )}

                {loading ? (
                    <div className="flp-spinner"><IonSpinner name="crescent" /></div>
                ) : (
                    <>
                        {/* ── Summary card ── */}
                        <div className="flp-card">
                            <div className="flp-summary-row">
                                <span>Quỹ đầu kỳ</span>
                                <span className="flp-val">{fmt(summary.opening)}</span>
                            </div>
                            <div className="flp-summary-row">
                                <span>Tổng thu</span>
                                <span className="flp-val">{fmt(summary.totalIn)}</span>
                            </div>
                            <div className="flp-summary-row">
                                <span>Tổng chi</span>
                                <span className={`flp-val ${summary.totalOut > 0 ? 'neg' : ''}`}>
                                    {summary.totalOut > 0 ? '-' : ''}{fmt(summary.totalOut)}
                                </span>
                            </div>
                            <div className="flp-summary-row flp-last-row">
                                <span>Tồn quỹ</span>
                                <span className={`flp-val ${summary.balance < 0 ? 'neg' : ''}`}>
                                    {summary.balance < 0 ? '' : ''}{fmt(summary.balance)}
                                </span>
                            </div>
                        </div>

                        {/* ── Transaction groups ── */}
                        {transactions.length === 0 ? (
                            <div className="flp-empty">Không có giao dịch nào</div>
                        ) : (
                            groups.keys.map(dateKey => (
                                <div key={dateKey} className="flp-group">
                                    <div className="flp-group-label">{dateKey}</div>
                                    {groups.map[dateKey].map(tx => {
                                        const { value, neg } = txAmount(tx);
                                        return (
                                            <div key={tx.id} className="flp-tx-row">
                                                <div className="flp-tx-info">
                                                    <div className="flp-tx-code">{txCode(tx)}</div>
                                                    <div className="flp-tx-desc">{txDesc(tx)}</div>
                                                    <div className="flp-tx-time">{fmtDateTime(tx.transactionTime || tx.createdAt)}</div>
                                                </div>
                                                <div className={`flp-tx-amount ${neg ? 'neg' : 'pos'}`}>{value}</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            ))
                        )}
                    </>
                )}

                {/* ── FAB ── */}
                {canCreateTransaction && (
                    <IonFab vertical="bottom" horizontal="end" slot="fixed">
                        <IonFabButton className="flp-fab">
                            <IonIcon icon={addOutline} />
                        </IonFabButton>
                    </IonFab>
                )}
            </IonContent>
        </IonPage>
    );
};

export default FundLedgerPage;
