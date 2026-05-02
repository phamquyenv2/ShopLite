import React, { useEffect, useRef, useState } from 'react';
import { IonIcon, IonSpinner, IonToast } from '@ionic/react';
import {
    addOutline,
    chevronBackOutline,
    searchOutline,
} from 'ionicons/icons';
import type { Unit } from '../api/types';
import { authApis, endpoints } from '../utils/Apis';
import './CategoryPickerModal.css'; // reuse same styles

interface UnitPickerModalProps {
    isOpen: boolean;
    selected: number | '';
    onClose: () => void;
    onSelect: (unit: Unit) => void;
}

type SubView = 'list' | 'add-new';

const UnitPickerModal: React.FC<UnitPickerModalProps> = ({
    isOpen,
    selected,
    onClose,
    onSelect,
}) => {
    const [view, setView] = useState<SubView>('list');
    const [units, setUnits] = useState<Unit[]>([]);
    const [loadingList, setLoadingList] = useState(false);
    const [keyword, setKeyword] = useState('');

    // Add-new form
    const [newName, setNewName] = useState('');
    const [newDesc, setNewDesc] = useState('');
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const searchRef = useRef<HTMLInputElement>(null);

    const reload = async () => {
        setLoadingList(true);
        try {
            const res = await authApis().get(endpoints.units);
            const raw = (res.data as { data?: Unit[] })?.data ?? res.data;
            if (Array.isArray(raw)) setUnits(raw as Unit[]);
        } finally {
            setLoadingList(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            setView('list');
            setKeyword('');
            setNewName('');
            setNewDesc('');
            void reload();
        }
    }, [isOpen]);

    const filtered = units.filter((u) =>
        u.name.toLowerCase().includes(keyword.toLowerCase())
    );

    const handleAddNew = async () => {
        if (!newName.trim()) {
            setToast('Vui lòng nhập tên đơn vị');
            return;
        }
        setSaving(true);
        try {
            const res = await authApis().post(endpoints.units, {
                name: newName.trim(),
                description: newDesc.trim() || null,
            });
            const raw = (res.data as { id?: number; data?: { id?: number; name?: string }; name?: string }) ?? {};
            const savedId: number =
                typeof (raw as { id?: number }).id === 'number'
                    ? (raw as { id: number }).id
                    : typeof (raw as { data?: { id?: number } }).data?.id === 'number'
                        ? (raw as { data: { id: number } }).data.id
                        : 0;
            const savedName: string =
                typeof (raw as { name?: string }).name === 'string'
                    ? (raw as { name: string }).name
                    : newName.trim();

            setToast('Thêm đơn vị thành công');
            setNewName('');
            setNewDesc('');
            await reload();
            if (savedId) {
                onSelect({ id: savedId, name: savedName });
            } else {
                setView('list');
            }
        } catch {
            setToast('Thêm đơn vị thất bại');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="cpm-overlay">
            <div className={`cpm-sheet ${isOpen ? 'cpm-sheet--in' : ''}`}>
                {/* ───── LIST VIEW ───── */}
                {view === 'list' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={onClose}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Chọn đơn vị tính</span>
                            <button
                                className="cpm-add-btn"
                                onClick={() => { setNewName(''); setNewDesc(''); setView('add-new'); }}
                            >
                                <IonIcon icon={addOutline} />
                            </button>
                        </div>

                        <div className="cpm-search-bar">
                            <IonIcon icon={searchOutline} className="cpm-search-icon" />
                            <input
                                ref={searchRef}
                                className="cpm-search-input"
                                placeholder="Tìm kiếm"
                                value={keyword}
                                onChange={(e) => setKeyword(e.target.value)}
                            />
                        </div>

                        <div className="cpm-list-wrap">
                            {loadingList ? (
                                <div className="cpm-center">
                                    <IonSpinner name="crescent" color="primary" />
                                </div>
                            ) : filtered.length === 0 ? (
                                <div className="cpm-empty">Không tìm thấy đơn vị tính</div>
                            ) : (
                                <div className="cpm-card">
                                    {filtered.map((unit, idx) => (
                                        <button
                                            key={unit.id}
                                            className={`cpm-item ${selected === unit.id ? 'cpm-item--active' : ''} ${idx < filtered.length - 1 ? 'cpm-item--divider' : ''}`}
                                            onClick={() => onSelect(unit)}
                                        >
                                            <span>{unit.name}</span>
                                            {selected === unit.id && (
                                                <span className="cpm-item-check">✓</span>
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}

                {/* ───── ADD NEW VIEW ───── */}
                {view === 'add-new' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={() => setView('list')}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Đơn vị tính mới</span>
                            <button
                                className="cpm-save-text-btn"
                                onClick={handleAddNew}
                                disabled={saving}
                            >
                                {saving ? <IonSpinner name="dots" /> : 'Lưu'}
                            </button>
                        </div>

                        <div className="cpm-form-card">
                            <div className="cpm-form-field">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Tên đơn vị *"
                                    value={newName}
                                    onChange={(e) => setNewName(e.target.value)}
                                    autoFocus
                                />
                            </div>
                            <div className="cpm-form-field cpm-form-field--last">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Mô tả"
                                    value={newDesc}
                                    onChange={(e) => setNewDesc(e.target.value)}
                                />
                            </div>
                        </div>
                    </>
                )}
            </div>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </div>
    );
};

export default UnitPickerModal;
