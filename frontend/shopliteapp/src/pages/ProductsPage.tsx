import {
    IonContent,
    IonFab,
    IonFabButton,
    IonHeader,
    IonIcon,
    IonButtons,
    IonButton,
    IonPage,
    IonRefresher,
    IonRefresherContent,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    addOutline,
    barcodeOutline,
    chevronBackOutline,
    imageOutline,
    searchOutline,
} from 'ionicons/icons';
import { useMemo, useState, useEffect } from 'react';
import { productService } from '../services/product.service';
import type { Product, Category } from '../api/types';
import './ProductsPage.css';

const formatVnd = (amount: number): string => `${new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(amount)))}`;

const ProductsPage: React.FC = () => {
    const ionRouter = useIonRouter();

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [toast, setToast] = useState<string | null>(null);

    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [activeCategoryId, setActiveCategoryId] = useState<number | 'ALL'>('ALL');
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');

    const loadData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [pList, cList] = await Promise.all([
                productService.getProducts({ size: 1000 }),
                productService.getCategories()
            ]);
            setProducts(pList);
            setCategories(cList);
        } catch (err: any) {
            setError(err.message || 'Không thể tải dữ liệu hàng hóa');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        void loadData();
    });

    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedSearchQuery(searchQuery);
        }, 500);
        return () => clearTimeout(handler);
    }, [searchQuery]);

    const filteredProducts = useMemo(() => {
        let result = products;

        // Apply category filter
        if (activeCategoryId !== 'ALL') {
            result = result.filter(p => p.categoryId === activeCategoryId);
        }

        // Apply search filter
        if (debouncedSearchQuery.trim().length > 0) {
            const q = debouncedSearchQuery.toLowerCase().trim();
            result = result.filter(p =>
                p.name?.toLowerCase().includes(q) ||
                p.sku?.toLowerCase().includes(q) ||
                p.barcode?.toLowerCase().includes(q)
            );
        }

        return result;
    }, [products, activeCategoryId, debouncedSearchQuery]);

    const getStockBadge = (stock: number) => {
        if (stock <= 0) {
            return { text: 'Hết hàng', className: 'products-stock-out' };
        }
        if (stock <= 10) {
            return { text: `Tồn: ${stock}`, className: 'products-stock-low' };
        }
        return { text: `Tồn: ${stock}`, className: 'products-stock-good' };
    };

    return (
        <IonPage>
            <IonHeader className="ion-no-border products-page-header">
                <IonToolbar className="products-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="products-toolbar-title">Hàng hóa</div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="products-content">
                <IonRefresher slot="fixed" onIonRefresh={async (e) => { await loadData(); e.detail.complete(); }}>
                    <IonRefresherContent />
                </IonRefresher>

                <div className="products-search-container">
                    <div className="products-search-box">
                        <IonIcon icon={searchOutline} />
                        <input
                            type="text"
                            placeholder="Tìm kiếm tên, mã sản phẩm..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                        />
                        <IonIcon icon={barcodeOutline} className="products-search-barcode" />
                    </div>
                </div>

                <div className="products-categories-scroll">
                    <button
                        className={`products-category-pill ${activeCategoryId === 'ALL' ? 'active' : ''}`}
                        onClick={() => setActiveCategoryId('ALL')}
                    >
                        Tất cả
                    </button>
                    {categories.map(cat => (
                        <button
                            key={cat.id}
                            className={`products-category-pill ${activeCategoryId === cat.id ? 'active' : ''}`}
                            onClick={() => setActiveCategoryId(cat.id)}
                        >
                            {cat.name}
                        </button>
                    ))}
                </div>

                {error && <div className="products-error">{error}</div>}

                {!loading && filteredProducts.length === 0 && !error && (
                    <div className="products-empty">Không tìm thấy sản phẩm nào</div>
                )}

                <div className="products-list">
                    {filteredProducts.map(p => {
                        const badge = getStockBadge(p.stock || 0);
                        return (
                            <div key={p.id} className="products-page-card" onClick={() => ionRouter.push(`/products/${p.id}`)}>
                                {p.image ? (
                                    <img src={p.image} alt={p.name} className="products-page-image" />
                                ) : (
                                    <div className="products-page-image">
                                        <IonIcon icon={imageOutline} />
                                    </div>
                                )}

                                <div className="products-page-info">
                                    <div className="products-page-name">{p.name}</div>
                                    <div className="products-page-code">Mã: {p.sku}</div>
                                </div>

                                <div className="products-page-meta">
                                    <div className="products-page-price">{formatVnd(p.sellingPrice)}</div>
                                    <div className={`products-page-stock-badge ${badge.className}`}>
                                        {badge.text}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>

                <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                    <IonFabButton className="products-fab" onClick={() => ionRouter.push('/product/new')}>
                        <IonIcon icon={addOutline} />
                    </IonFabButton>
                </IonFab>

            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ProductsPage;
