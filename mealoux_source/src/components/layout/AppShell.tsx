import { useMemo } from 'react'
import { Outlet } from 'react-router-dom'
import { useUnseenCount } from '@/lib/notifications'
import { PillNav, ToastProvider, type PillNavItem } from '@/components/ui'
import { PhoneFrame } from './PhoneFrame'
import { t } from '@/i18n'

const baseItems: PillNavItem[] = [
  { to: '/', label: t.nav.home, icon: 'home', end: true },
  { to: '/explorer', label: t.nav.explore, icon: 'grid' },
  { to: '/profil', label: t.nav.profile, icon: 'user' },
]

export function AppShell() {
  // Pastille rouge sur Profil tant qu'une activité de session n'a pas été vue
  const unseen = useUnseenCount()
  const items = useMemo(() => baseItems.map((item) => (item.to === '/profil' ? { ...item, badge: unseen > 0, badgeLabel: t.sessions.notification } : item)), [unseen])
  return (
    <PhoneFrame>
      <ToastProvider>
        <main className="flex min-h-dvh flex-col px-[15px] pb-[125px] pt-6 sm:min-h-full sm:pt-[70px]">
          <Outlet />
        </main>
        <PillNav items={items} action={{ to: '/creer', label: t.nav.create }} />
      </ToastProvider>
    </PhoneFrame>
  )
}
