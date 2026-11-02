import { HashRouter, Route, Routes } from 'react-router-dom'
import { FullScreenShell } from '@/components/layout/FullScreenShell'
import { AppShell } from '@/components/layout/AppShell'
import { t } from '@/i18n'
import ComingSoonPage from '@/pages/ComingSoonPage'
import ExplorePage from '@/pages/ExplorePage'
import CreateSessionPage from '@/pages/CreateSessionPage'
import JoinPage from '@/pages/JoinPage'
import ResultsPage from '@/pages/ResultsPage'
import SessionPage from '@/pages/SessionPage'
import VotePage from '@/pages/VotePage'
import ReviewPage from '@/pages/ReviewPage'
import RestaurantPage from '@/pages/RestaurantPage'
import SessionsPage from '@/pages/SessionsPage'
import ProfileListPage from '@/pages/ProfileListPage'
import ProfileReviewsPage from '@/pages/ProfileReviewsPage'
import ProfilePage from '@/pages/ProfilePage'
import HomePage from '@/pages/HomePage'
import KitPage from '@/pages/KitPage'

export default function App() {
  return (
    <HashRouter>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<HomePage />} />
            <Route path="explorer" element={<ExplorePage />} />
            <Route path="profil" element={<ProfilePage />} />
            <Route path="profil/sessions" element={<SessionsPage />} />
            <Route path="profil/aimes" element={<ProfileListPage kind="liked" />} />
            <Route path="profil/a-essayer" element={<ProfileListPage kind="toTry" />} />
            <Route path="profil/avis" element={<ProfileReviewsPage />} />
            <Route path="recap" element={<ComingSoonPage title={t.recap.title} text={t.recap.text} mascot="tiramisu" />} />
            <Route path="kit" element={<KitPage />} />
            <Route path="*" element={<ComingSoonPage />} />
          </Route>
        <Route element={<FullScreenShell />}>
            <Route path="restaurant/:id" element={<RestaurantPage />} />
            <Route path="restaurant/:id/avis" element={<ReviewPage />} />
            <Route path="creer" element={<CreateSessionPage />} />
            <Route path="rejoindre" element={<JoinPage />} />
            <Route path="s/:code" element={<SessionPage />} />
            <Route path="s/:code/vote" element={<VotePage />} />
            <Route path="s/:code/resultat" element={<ResultsPage />} />
          </Route>
        </Routes>
    </HashRouter>
  )
}
