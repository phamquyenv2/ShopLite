import React, { useEffect, useState } from 'react';
import {
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
    IonSpinner
} from '@ionic/react';
import {
    arrowBackOutline,
    ellipsisVerticalOutline,
    cubeOutline,
    informationCircleOutline,
    timeOutline,
    chevronForwardOutline,
    pencilOutline,
    addCircleOutline,
    clipboardOutline,
    imageOutline
} from 'ionicons/icons';
import { useParams } from 'react-router-dom';
import { productService } from '../services/product.service';
import type { Product } from '../api/types';
import './ProductDetailPage.css';

const formatVnd = (amount: number): string => `${new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(amount)))}`;

const ProductDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();

    const [product, setProduct] = useState<Product | null>(null);
    const [loading, setLoading] = useState(true);

    useIonViewWillEnter(() => {
        const load = async () => {
            setLoading(true);
            if (id) {
                const p = await productService.getProductDetail(id);
                setProduct(p);
            }
            setLoading(false);
        };
        void load();
    });

    if (loading) {
        return (
            <IonPage>
                <IonHeader className="ion-no-border product-detail-header">
                    <IonToolbar className="product-detail-toolbar">
                        <div className="products-toolbar-left" slot="start">
                            <button className="product-detail-icon" type="button" aria-label="Back" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={arrowBackOutline} />
                            </button>
                            <div className="product-detail-title">Chi tiết hàng hóa</div>
                        </div>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="product-detail-content">
                    <div style={{ textAlign: 'center', marginTop: '40px' }}>
                        <IonSpinner />
                    </div>
                </IonContent>
            </IonPage>
        );
    }

    if (!product) {
        return (
            <IonPage>
                <IonHeader className="ion-no-border product-detail-header">
                    <IonToolbar className="product-detail-toolbar">
                        <div className="products-toolbar-left" slot="start">
                            <button className="product-detail-icon" type="button" aria-label="Back" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={arrowBackOutline} />
                            </button>
                            <div className="product-detail-title">Chi tiết hàng hóa</div>
                        </div>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="product-detail-content">
                    <div style={{ textAlign: 'center', marginTop: '40px', color: '#888' }}>
                        Không tìm thấy thông tin sản phẩm
                    </div>
                </IonContent>
            </IonPage>
        );
    }

    const isActive = product.status === 'ACTIVE' || !product.status; // Default assuming active if no status

    const costPrice = product.costPrice || 0;
    const sellingPrice = product.sellingPrice || 0;
    const profit = sellingPrice - costPrice;
    const profitPercent = costPrice > 0 ? ((profit / costPrice) * 100).toFixed(0) : '0';

    return (
        <IonPage className="product-detail-page">
            <IonHeader className="ion-no-border product-detail-header">
                <IonToolbar className="product-detail-toolbar">
                    <div className="products-toolbar-left" slot="start" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <button className="product-detail-icon" type="button" aria-label="Back" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                        <div className="product-detail-title">Chi tiết hàng hóa</div>
                    </div>
                    <div className="products-toolbar-right" slot="end">
                        <button className="product-detail-icon" type="button" aria-label="Options">
                            <IonIcon icon={ellipsisVerticalOutline} />
                        </button>
                    </div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="product-detail-content">
                <div className="product-detail-cards">

                    {/* Main Info Box */}
                    <div className="pd-card">
                        {product.image ? (
                            <img src={product.image} alt={product.name} className="pd-image-banner" />
                        ) : (
                            <div className="pd-image-banner">
                                <IonIcon icon={imageOutline} />
                            </div>
                        )}

                        <div className="pd-title-row">
                            <h2 className="pd-title">{product.name}</h2>
                            <div className={`pd-status ${isActive ? 'active' : 'inactive'}`}>
                                {isActive ? 'Đang bán' : 'Ngừng bán'}
                            </div>
                        </div>

                        <div className="pd-info-row">
                            <span className="pd-info-label">Mã hàng:</span>
                            <span className="pd-info-value">{product.sku || '---'}</span>
                        </div>
                        <div className="pd-info-row">
                            <span className="pd-info-label">Nhóm hàng:</span>
                            <span className="pd-info-value">{product.categoryName || '---'}</span>
                        </div>
                        <div className="pd-info-row">
                            <span className="pd-info-label">SKU:</span>
                            <span className="pd-info-value">{product.sku || '---'}</span>
                        </div>
                    </div>

                    {/* Price and Stock Box */}
                    <div className="pd-card">
                        <div className="pd-section-header">
                            <div className="pd-section-icon"><IonIcon icon={cubeOutline} /></div>
                            Giá & Tồn kho
                        </div>

                        <div className="pd-price-row">
                            <span className="pd-info-label">Giá bán</span>
                            <span className="pd-price-val blue">{formatVnd(sellingPrice)}đ</span>
                        </div>
                        <div className="pd-price-row">
                            <span className="pd-info-label">Giá vốn</span>
                            <span className="pd-price-val">{formatVnd(costPrice)}đ</span>
                        </div>

                        <div className="pd-stock-boxes">
                            <div className="pd-stock-box">
                                <span className="pd-stock-box-label">HIỆN TẠI</span>
                                <span className="pd-stock-box-val">{product.stock || 0}</span>
                                <span className="pd-stock-box-unit">{product.unitName || '---'}</span>
                            </div>
                            <div className="pd-stock-box">
                                <span className="pd-stock-box-label">TỐI THIỂU</span>
                                <span className="pd-stock-box-val">{product.minStock ?? '0'}</span>
                                <span className="pd-stock-box-unit">&nbsp;</span>
                            </div>
                            <div className="pd-stock-box">
                                <span className="pd-stock-box-label">TỐI ĐA</span>
                                <span className="pd-stock-box-val">{product.maxStock ?? '0'}</span>
                                <span className="pd-stock-box-unit">&nbsp;</span>
                            </div>
                        </div>
                    </div>

                    {/* Detailed Info Box */}
                    <div className="pd-card">
                        <div className="pd-section-header">
                            <div className="pd-section-icon" style={{ borderRadius: '50%' }}><IonIcon icon={informationCircleOutline} /></div>
                            Thông tin chi tiết
                        </div>

                        <div className="pd-info-row">
                            <span className="pd-info-label">Mã vạch</span>
                            <span className="pd-info-value">{product.barcode || '---'}</span>
                        </div>
                        <div className="pd-info-row">
                            <span className="pd-info-label">Đơn vị tính</span>
                            <span className="pd-info-value">{product.unitName || '---'}</span>
                        </div>
                        <div className="pd-info-row">
                            <span className="pd-info-label">Nhóm hàng</span>
                            <span className="pd-info-value">{product.categoryName || '---'}</span>
                        </div>

                        <div className="pd-profit-banner">
                            <div className="pd-profit-left">
                                <span className="pd-profit-label">LỢI NHUẬN DỰ TÍNH</span>
                                <span className="pd-profit-val">{formatVnd(profit)}đ</span>
                            </div>
                            <div className="pd-profit-percent">{profitPercent}%</div>
                        </div>
                    </div>

                    {/* History */}
                    <div className="pd-card" style={{ padding: 0, overflow: 'hidden' }}>
                        <div className="pd-history-card">
                            <div className="pd-history-left">
                                <IonIcon icon={timeOutline} className="pd-history-icon" />
                                Lịch sử xuất/nhập
                            </div>
                            <IonIcon icon={chevronForwardOutline} style={{ color: '#aaa', fontSize: '20px' }} />
                        </div>
                        <div className="pd-history-empty">
                            <div className="pd-history-empty-icon">
                                <IonIcon icon={clipboardOutline} />
                            </div>
                            Chưa có lịch sử xuất/nhập
                        </div>
                    </div>
                </div>

            </IonContent>

            <div className="pd-bottom-bar">
                <button className="pd-btn pd-btn-outline">
                    <IonIcon icon={pencilOutline} />
                    Chỉnh sửa
                </button>
                <button className="pd-btn pd-btn-solid">
                    <IonIcon icon={addCircleOutline} />
                    Nhập hàng
                </button>
            </div>
        </IonPage>
    );
};

export default ProductDetailPage;
