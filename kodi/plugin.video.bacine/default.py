# -*- coding: utf-8 -*-
"""
Passerelle BaCine -> Kodi.

BaCine ouvre Kodi avec un intent Android ACTION_VIEW portant une URL
plugin://plugin.video.bacine/?action=search&title=...

Kodi traite toute URL reçue par intent comme une demande de *lecture*
(TMSG_MEDIA_PLAY) : il ne sait pas y ouvrir un dossier. Or la recherche
globale de vStream est un dossier. Ce module sert donc d'aiguillage : il
annule la lecture demandée, puis ouvre la recherche vStream dans la fenêtre
Vidéos, comme si l'utilisateur l'avait lancée depuis vStream.

Passer par ce petit add-on plutôt que modifier vStream laisse vStream
intact : ses mises à jour automatiques n'écrasent rien.
"""
import sys
from urllib.parse import parse_qsl, quote

import xbmc
import xbmcgui
import xbmcplugin

VSTREAM_ID = 'plugin.video.vstream'

# Catégorie « Films » de la recherche globale vStream (2 = séries).
VSTREAM_CAT_MOVIES = '1'


def vstream_search_url(title):
    """URL de la recherche globale vStream pour un titre de film.

    vStream découpe ses paramètres sur « & » et « = » avant de les décoder :
    le titre doit être entièrement encodé, sans aucun caractère réservé.
    """
    return 'plugin://%s/?site=globalSearch&function=globalSearch&searchtext=%s&sCat=%s' % (
        VSTREAM_ID, quote(title, safe=''), VSTREAM_CAT_MOVIES)


def notify(message):
    xbmcgui.Dialog().notification('BaCine', message, xbmcgui.NOTIFICATION_WARNING, 5000)


def search(handle, title):
    # Kodi attend une réponse à la « lecture » qu'il a lancée : on la refuse
    # avant tout, sinon il reste bloqué sur son indicateur de chargement.
    if handle >= 0:
        xbmcplugin.setResolvedUrl(handle, False, xbmcgui.ListItem())

    title = title.strip()
    if not title:
        return

    if not xbmc.getCondVisibility('System.HasAddon(%s)' % VSTREAM_ID):
        notify("vStream n'est pas installé dans Kodi")
        return

    # « return » : Retour depuis les résultats ramène à l'écran précédent au
    # lieu de remonter dans l'arborescence de vStream.
    xbmc.executebuiltin('ActivateWindow(Videos,"%s",return)' % vstream_search_url(title))


def main(argv):
    handle = int(argv[1]) if len(argv) > 1 and argv[1].lstrip('-').isdigit() else -1
    params = dict(parse_qsl(argv[2].lstrip('?'))) if len(argv) > 2 else {}

    if params.get('action') == 'search':
        search(handle, params.get('title', ''))
        return

    # Ouvert depuis le menu des add-ons de Kodi : rien à afficher.
    if handle >= 0:
        xbmcplugin.endOfDirectory(handle, succeeded=False)
    notify("Cet add-on s'utilise depuis l'application BaCine")


if __name__ == '__main__':
    main(sys.argv)
