import { useEffect } from 'react';

/**
 * Chạy `load` sau khi render, và chạy lại mỗi khi `load` đổi (tức khi deps của useCallback đổi).
 *
 * `load` được hoãn sang microtask nên các setState đồng bộ bên trong (vd. setLoading(true)) không chạy
 * ngay trong effect — tránh render dây chuyền (quy tắc react-hooks/set-state-in-effect). Lần chạy bị bỏ
 * qua nếu component đã unmount hoặc `load` đã đổi trước khi kịp chạy (StrictMode chỉ tải một lần).
 */
export default function useLoadEffect(load) {
    useEffect(() => {
        let active = true;
        queueMicrotask(() => {
            if (active) load();
        });
        return () => {
            active = false;
        };
    }, [load]);
}
