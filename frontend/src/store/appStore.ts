import { create } from 'zustand';

interface AppState {
  selectedBreachId: string | null;
  setSelectedBreachId: (id: string | null) => void;
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  resetAppState: () => void;
}

export const useAppStore = create<AppState>((set) => ({
  selectedBreachId: null,
  setSelectedBreachId: (id) => set({ selectedBreachId: id }),
  sidebarCollapsed: false,
  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  resetAppState: () =>
    set({
      selectedBreachId: null,
      sidebarCollapsed: false,
    }),
}));
