package ca.panierfute.i18n

import ca.panierfute.data.Endroit
import ca.panierfute.data.Langue
import ca.panierfute.data.Moment
import ca.panierfute.data.Plan
import ca.panierfute.data.Rayon
import ca.panierfute.data.Zone
import ca.panierfute.ui.finJoursIdeaux
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Español neutro de América, con tuteo. */
object TextesEs : Textes(Langue.ES) {
    private fun pl(n: Int, uno: String, varios: String) = if (n == 1) uno else varios

    /** « jueves 19 de noviembre » */
    private fun diaYFecha(date: LocalDate) = d(date, "EEEE d 'de' MMMM")
    private fun diaYFechaCorta(date: LocalDate) = d(date, "EEE d 'de' MMM")

    // ----- Dates -----
    override fun dateHeure(z: ZonedDateTime) =
        z.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMM, HH:mm", langue.locale))
    override fun heure(h: Int, m: Int) = h.toString().padStart(2, '0') + ":" + m.toString().padStart(2, '0')
    override fun jourCourt(date: LocalDate) = d(date, "EEE")
    override fun jourAbrege(j: DayOfWeek) = when (j) {
        DayOfWeek.MONDAY -> "Lun"; DayOfWeek.TUESDAY -> "Mar"; DayOfWeek.WEDNESDAY -> "Mié"
        DayOfWeek.THURSDAY -> "Jue"; DayOfWeek.FRIDAY -> "Vie"; DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }
    override fun initialeJour(date: LocalDate) = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "L"; DayOfWeek.TUESDAY -> "M"; DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"; DayOfWeek.FRIDAY -> "V"; DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
    override val aujourdhuiCourt = "hoy"

    // ----- Onglets -----
    override val ongletSemaine = "Semana"
    override val ongletEpicerie = "Compras"
    override val ongletPhotos = "Fotos"
    override val ongletReglages = "Ajustes"

    // ----- Communs -----
    override val ajouter = "Agregar"
    override val retirer = "Quitar"
    override val modifier = "Editar"
    override val enregistrer = "Guardar"
    override val annuler = "Cancelar"
    override val masquer = "Ocultar"
    override val special = "Oferta"
    override val genererPlan = "Crear mi plan ahora"
    override fun prochainRappel(quand: String) = "Próximo recordatorio: $quand"
    override fun prochaineGeneration(quand: String) = "Próximo plan: $quand"

    // ----- Circulaire -----
    override fun circulairePeriode(debut: LocalDate, fin: LocalDate) =
        if (debut.month == fin.month) "Ofertas del ${d(debut, "EEEE d")} al ${diaYFecha(fin)}"
        else "Ofertas del ${diaYFecha(debut)} al ${diaYFecha(fin)}"
    override fun circulaireCourte(debut: LocalDate, fin: LocalDate) =
        if (debut.month == fin.month) "Folleto del ${debut.dayOfMonth} al ${d(fin, "d 'de' MMM")}"
        else "Folleto del ${d(debut, "d 'de' MMM")} al ${d(fin, "d 'de' MMM")}"
    override fun circulaireJusquau(fin: LocalDate) = "Ofertas válidas hasta el ${diaYFecha(fin)}"
    override fun circulaireAVenir(debut: LocalDate, idealFin: LocalDate) =
        "Las ofertas empiezan el ${diaYFecha(debut)}. Lo ideal es ir antes del ${diaYFecha(idealFin)}, antes de que se agoten."
    override fun circulaireIdeal(idealFin: LocalDate) =
        "Lo ideal es ir antes del ${diaYFecha(idealFin)}: las ofertas se agotan rápido los primeros días."
    override fun circulaireRestant(jours: Int, fin: LocalDate) =
        if (jours <= 1) "Último día de ofertas. Ve lo antes posible."
        else "Quedan $jours días de ofertas, hasta el ${diaYFecha(fin)}. Ve lo antes posible: algunos productos quizá ya se agotaron."
    override val circulaireTerminee = "Este folleto terminó. Crea un plan nuevo para aprovechar el siguiente."

    // ----- Semaine -----
    override val titreSemaine = "Tu semana"
    override fun epicerieEtCodePostal(marchand: String, codePostal: String) = "$marchand, $codePostal"
    override val attenteReseau = "Esperando conexión a internet"
    override val generationEchouee = "No se pudo crear el último plan"
    override fun versionDemandee(consignes: String) = "Pediste: «$consignes»"
    override val titreRepas = "En el menú"
    override val titreRemarques = "Para tener en cuenta"
    override val preparationPlan = "Preparando tu plan"
    override val fermerAppPossible = "Puedes cerrar la app: te avisaremos con una notificación."
    override val accueilIntro =
        "Cada semana, Panier Futé lee el folleto de tu supermercado, revisa lo que ya tienes y te propone comidas y una lista de compras que aprovechan las ofertas."
    override val etapeEpicerie = "Elige tu supermercado"
    override val etapeCle = "Configura el servicio de IA"
    override val etapePhotos = "Toma fotos de tu despensa, refrigerador y congelador"
    override fun nbPhotos(n: Int) = "$n " + pl(n, "foto", "fotos")
    override fun horaireResume(rappel: String, generation: String) =
        "Próximo recordatorio: $rappel. Plan automático: $generation."
    override val epicerieEstimee = "Compras estimadas, impuestos incluidos"
    override fun articlesResume(n: Int, enSpecial: Int) =
        "$n " + pl(n, "artículo", "artículos") + ", $enSpecial en oferta"
    override fun economies(montant: String) = "Ahorro gracias a las ofertas: $montant"
    override fun moment(m: Moment) = when (m) {
        Moment.DEJEUNER -> "Desayuno"
        Moment.DINER -> "Almuerzo"
        Moment.SOUPER -> "Cena"
    }
    override val voirRecette = "Ver la receta"
    override val masquerRecette = "Ocultar la receta"
    override fun ingredientsPour(portions: Int) =
        if (portions > 0) "Ingredientes para $portions " + pl(portions, "porción", "porciones") else "Ingredientes"
    override val sourceRabais = "en oferta"
    override val sourceStock = "en casa"
    override val sourceBase = "básico"
    override val sourceAchat = "por comprar"
    override val preparation = "Preparación"
    override fun titreInventaire(n: Int) = "En casa ($n)"
    override val inventaireAide =
        "Lo que la IA vio en tus fotos. ¿Algún error u olvido? Corrígelo aquí y luego crea un plan nuevo."
    override val inventaireCorrige = "Corregido por ti. Crea un plan nuevo para que las comidas lo tomen en cuenta."
    override val voirEtCorriger = "Ver y corregir"
    override val inventaireVide = "Todavía no hay alimentos."
    override fun endroit(e: Endroit) = when (e) {
        Endroit.GARDE_MANGER -> "Despensa"
        Endroit.FRIGO -> "Refrigerador"
        Endroit.CONGELATEUR -> "Congelador"
        Endroit.AUTRE -> "Otro"
    }
    override val utiliserVite = "Usar pronto"
    override val ajouterAliment = "Agregar un alimento"
    override val modifierAliment = "Editar el alimento"
    override val aliment = "Alimento"
    override val quantiteFacultative = "Cantidad (opcional)"
    override val exempleQuantite = "Ej.: 500 g, unos 4, medio frasco"
    override val titreRegenerer = "¿No es lo que buscabas?"
    override val regenererAide = "Dile a la IA qué cambiar. Vuelve a partir de tu inventario y del folleto."
    override val nouvellesPreferences = "Nuevas preferencias (opcional)"
    override val exempleConsignes = "Ej.: más vegetariano, sin cerdo, presupuesto ajustado, deja la cena del sábado"
    override val garderPreferences = "Guardar estas preferencias para las próximas semanas"
    override val regenerer = "Crear un plan nuevo"
    override val regenererTitre = "¿Crear un plan nuevo?"
    override fun regenererTexte(nouvellesPhotos: Int) =
        if (nouvellesPhotos > 0) {
            "Se analizarán tus ${nbPhotos(nouvellesPhotos)}. El plan nuevo reemplaza al actual y a su lista de compras."
        } else {
            "Sin fotos nuevas, la IA parte de tu inventario, con tus correcciones. El plan nuevo reemplaza al actual y a su lista de compras."
        }
    override fun coutIA(fournisseur: String, cents: Double, photos: Int, articles: Int) =
        "Análisis de $fournisseur: " + String.format(langue.locale, "%.2f", cents) + " centavos de dólar, " +
            nbPhotos(photos) + " y $articles productos del folleto leídos."
    override fun jetonsIA(fournisseur: String, entree: Int, sortie: Int, photos: Int, articles: Int) =
        "Análisis de $fournisseur: $entree tokens enviados, $sortie recibidos, " + nbPhotos(photos) +
            " y $articles productos del folleto leídos."

    // ----- Épicerie -----
    override val titreEpicerie = "Compras"
    override val epicerieVide = "Tu lista de compras aparecerá aquí cuando el plan de la semana esté listo."
    override val allerASemaine = "Ir a Tu semana"
    override val totalTaxesIncluses = "Total estimado, impuestos incluidos"
    override fun sousTotal(montant: String) = "Subtotal: $montant"
    override fun taxes(montant: String, pourcentage: Double, nbTaxables: Int) =
        "Impuestos de ${pourcent(pourcentage)}: $montant, sobre $nbTaxables " + pl(nbTaxables, "artículo gravado", "artículos gravados")
    override fun dansLePanier(faits: Int, total: Int) = "$faits de $total en el carrito"
    override val partager = "Compartir"
    override val partagerListe = "Compartir la lista de compras"
    override val toutDecocher = "Desmarcar todo"
    override fun rayon(r: Rayon) = when (r) {
        Rayon.FRUITS_LEGUMES -> "Frutas y verduras"
        Rayon.BOULANGERIE -> "Panadería"
        Rayon.VIANDES -> "Carnes y pescados"
        Rayon.LAITIERS -> "Lácteos y huevos"
        Rayon.EPICERIE -> "Despensa"
        Rayon.SURGELES -> "Congelados"
        Rayon.AUTRES -> "Otros"
    }
    override val plusTaxes = "+ impuestos"
    override val prixEstimesNote =
        "Los precios con ≈ son estimados por la IA. Los impuestos solo se aplican a los artículos gravados: la mayoría de los alimentos básicos están exentos."

    // ----- Photos -----
    override val titrePhotos = "Fotos"
    override val photosIntro =
        "Toma fotos de lo que ya tienes. La IA las usa para no comprar de más y aprovechar lo que hay que comer pronto."
    override fun zone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Despensa"
        Zone.FRIGO -> "Refrigerador"
        Zone.CONGELO -> "Congelador"
    }
    override fun conseilZone(z: Zone) = when (z) {
        Zone.GARDE_MANGER -> "Abre las puertas, una foto por repisa si hace falta."
        Zone.FRIGO -> "Incluye la puerta y los cajones de verduras."
        Zone.CONGELO -> "Saca las bolsas del fondo si puedes."
    }
    override val prendrePhoto = "Tomar una foto"
    override val galerie = "Galería"
    override val supprimerPhoto = "Borrar la foto"
    override val photosAucune = "Todavía no hay fotos. Sin fotos, el plan solo cuenta con tus básicos."
    override fun photosPretes(n: Int, quand: String) =
        "${nbPhotos(n)} " + pl(n, "lista", "listas") +
            ". Se analizarán en el plan automático ($quand) o cuando crees el plan, y luego se borrarán."
    override val aucunAppareilPhoto = "No se encontró una app de cámara."
    override val galerieImpossible = "No se puede abrir la galería."

    // ----- Réglages -----
    override val titreReglages = "Ajustes"
    override val reglagesIntro = "Todo se guarda automáticamente."
    override val langueTitre = "Idioma"
    override val epicerieTitre = "Mi supermercado"
    override val epicerieAide =
        "Escribe tu código postal y elige tu supermercado entre los que publican un folleto cerca de ti. Canadá y Estados Unidos."
    override val codePostal = "Código postal"
    override val exempleCodePostal = "10001"
    override val chercher = "Buscar"
    override val rechercheEnCours = "Buscando folletos"
    override fun epiceriesTrouvees(n: Int) = "$n " + pl(n, "supermercado con folleto", "supermercados con folleto")
    override val choisie = "Supermercado elegido"
    override val afficherToutesEpiceries = "Mostrar otras tiendas"
    override val afficherToutesAide = "Grandes tiendas y farmacias que también venden alimentos"
    override val taxesTitre = "Impuestos sobre artículos gravados"
    override fun taxesAide(pourcentage: String, region: String) =
        "Tasa de tu región: $pourcentage % ($region). Déjalo vacío para usarla o escribe tu propia tasa."
    override val tauxTaxes = "Tasa personalizada (%)"
    override fun circulaireChangeLe(jour: DayOfWeek) = "Tu folleto cambia el ${nomJour(jour)}"
    override val alignerAide =
        "Para aprovechar las ofertas desde el primer día: recordatorio de fotos la víspera a las 19:00 y plan creado a las 00:30 el día que cambia el folleto."
    override val alignerHoraire = "Ajustar el horario al folleto"
    override val horaireDejaAligne = "Tu horario sigue el folleto: el plan está listo el primer día de ofertas."
    override val iaTitre = "Servicio de IA"
    override val iaAide =
        "Elige el servicio de IA y pega tu clave de API. DeepSeek es el más barato. El modelo debe poder leer imágenes."
    override val fournisseurAutre = "Otro"
    override val urlApi = "Dirección de la API"
    override val urlApiAide = "Cualquier API compatible con OpenAI (chat completions)."
    override fun cleApi(fournisseur: String) = "Clave de API de $fournisseur"
    override fun ouTrouverCle(site: String) = "Crea tu clave en $site."
    override val voir = "Ver"
    override val cacher = "Ocultar"
    override val modele = "Modelo"
    override val modeleAide = "Déjalo vacío para usar el modelo sugerido."
    override val modeReflexion = "Modo de razonamiento"
    override val modeReflexionAide = "Planes un poco mejor pensados, pero con cerca del doble de tokens."
    override val tester = "Probar"
    override val repasTitre = "Mis comidas"
    override val portions = "Porciones por comida"
    override val soupers = "Cenas"
    override val diners = "Almuerzos"
    override val dinersAide = "A menudo con las sobras de la cena"
    override val dejeuners = "Desayunos"
    override val preferences = "Preferencias, alergias, restricciones"
    override val exemplePreferences = "Ej.: sin mariscos, dos noches vegetarianas, nos gusta el picante"
    override fun ingredientsBaseTitre(n: Int) = "Básicos ($n)"
    override val ingredientsBaseAide =
        "Siempre en casa, aunque no salgan en las fotos. Nunca se agregan a la lista de compras. Toca uno para quitarlo."
    override val ajouterIngredients = "Agregar (separados por comas)"
    override val retablirListe = "Restablecer la lista inicial"
    override val horaireTitre = "Horario semanal"
    override val rappelPhotos = "Recordatorio de fotos"
    override fun heureRappel(h: String) = "Recordatorio a las $h"
    override val generationAuto = "Plan automático"
    override fun heureGeneration(h: String) = "Plan a las $h"
    override val genererSansPhotos = "Crear el plan aunque no haya fotos"
    override val genererSansPhotosAide = "Si no, el plan automático espera tus fotos."
    override val fiabiliteTitre = "Notificaciones en Samsung Galaxy"
    override val fiabiliteTexte =
        "Samsung a veces pone las apps en reposo, lo que retrasa el recordatorio y el plan nocturno. En Ajustes, Batería, Límites de uso en segundo plano, agrega Panier Futé a las apps que nunca entran en reposo."
    override val ouvrirReglagesBatterie = "Abrir ajustes de batería"

    // ----- Messages -----
    override val epicerieDAbord = "Primero elige tu supermercado."
    override val cleDAbord = "Primero configura el servicio de IA."
    override val generationLancee = "Plan en marcha. Puedes cerrar la app: te avisaremos con una notificación."
    override val photoNonEnregistree = "No se pudo guardar la foto."
    override fun photosAjoutees(n: Int, max: Int) =
        "${nbPhotos(n)} " + pl(n, "agregada", "agregadas") + ". Máximo $max por lugar."
    override val entrerCleDAbord = "Primero escribe tu clave de API (y la dirección de la API para Otro)."
    override val verification = "Verificando"
    override fun testReussi(modele: String, solde: String?) =
        "Funciona: $modele responde y lee imágenes." + (solde?.let { " Saldo: $it." } ?: "")
    override val erreurInconnue = "Error desconocido."
    override val erreurCodePostal = "Este código no es válido. Ejemplo: 10001 en Estados Unidos, G1S 1T7 en Canadá."
    override val aucuneEpicerieTrouvee = "No se encontraron folletos para este código."
    override fun epicerieAbsente(nom: String) = "$nom no tiene folleto vigente cerca de este código. Elige otro supermercado."
    override val erreurRechercheEpiceries = "No se pudo cargar la lista de supermercados. Revisa tu conexión e inténtalo de nuevo."
    override fun epicerieChoisieMessage(nom: String) = "Elegiste $nom. Tus próximos planes usarán su folleto."
    override fun horaireAligne(jour: DayOfWeek) =
        "Horario ajustado: recordatorio la víspera a las 19:00, plan el ${nomJour(jour)} a las 00:30."

    // ----- Génération -----
    override val aucunePhotoSemaine = "No hubo fotos esta semana."
    override fun progresCirculaire(marchand: String) = "Leyendo el folleto de $marchand"
    override val progresDemande = "Preparando la solicitud"
    override fun progresPhotos(n: Int) = if (n == 1) "Preparando la foto" else "Preparando $n fotos"
    override val progresIA = "Análisis de la IA, de 1 a 3 minutos"
    override val progresNouvelEssai = "Respuesta incompleta, nuevo intento"
    override val progresNouvelEssaiLong = "Respuesta demasiado larga, nuevo intento"
    override val progresEnregistrement = "Guardando el plan"

    // ----- Erreurs -----
    override val erreurAucuneEpicerie = "Primero elige tu supermercado en Ajustes."
    override fun erreurCirculaireIntrouvable(marchand: String, codePostal: String) =
        "No se encontró un folleto de $marchand cerca de $codePostal. Elige otro supermercado en Ajustes."
    override fun erreurCirculaireVide(marchand: String) =
        "El folleto de $marchand está vacío o no se puede leer por ahora. Inténtalo más tarde."
    override val erreurCleManquante = "Agrega tu clave de API en Ajustes, en Servicio de IA."
    override val erreurUrlManquante = "Escribe la dirección de la API en Ajustes."
    override val erreurIASansReponse = "La IA no respondió. Inténtalo en unos minutos."
    override val erreurEnregistrement = "No se pudo guardar el plan."
    override fun erreurIA401(nom: String) = "$nom rechazó la clave de API. Revísala en Ajustes."
    override fun erreurIA402(nom: String) = "Saldo insuficiente en $nom. Recarga tu cuenta."
    override fun erreurIA404(nom: String, modele: String) = "$nom no encuentra el modelo «$modele». Revisa su nombre en Ajustes."
    override fun erreurIA429(nom: String) = "Demasiadas solicitudes a $nom. Inténtalo en unos minutos."
    override fun erreurIAServeur(nom: String, code: Int) = "Los servidores de $nom no responden (error $code). Inténtalo más tarde."
    override fun erreurIAAutre(nom: String, code: Int, detail: String) = "$nom rechazó la solicitud (error $code): $detail"
    override fun diagnosticCoupee(jetons: Int) = "respuesta cortada después de $jetons tokens"
    override fun diagnosticIllisible(caracteres: Int) = "respuesta ilegible de $caracteres caracteres"
    override fun diagnosticReflexion(jetons: Int) = "de ellos $jetons de razonamiento"
    override val diagnosticEspaces = "casi todo espacios en blanco"
    override fun erreurPlanIncomplet(essais: Int, details: String) =
        "La IA no devolvió un plan completo después de $essais " + pl(essais, "intento", "intentos") +
            " ($details). Inténtalo de nuevo. Si vuelve a pasar, elige otro modelo en Ajustes."

    // ----- Notifications -----
    override val notifCanalNom = "Compras de la semana"
    override val notifCanalDescription = "Recordatorio de fotos y plan de comidas listo"
    override val notifRappelTitre = "Fotos del refrigerador, por favor"
    override fun notifRappelTexte(dejaPrises: Int, quand: String) =
        if (dejaPrises > 0) "${nbPhotos(dejaPrises)} " + pl(dejaPrises, "lista", "listas") + ". Agrega más si quieres: el plan se creará el $quand."
        else "Toma fotos de tu despensa, refrigerador y congelador. El plan se creará el $quand."
    override val notifPlanTitre = "Tu plan de la semana está listo"
    override fun notifPlanTexte(plan: Plan): String {
        val base = "${plan.repas.size} comidas y ${plan.epicerie.size} artículos, unos ${arg(plan.totalEstime)} con impuestos."
        val debut = plan.circulaireDebut
        val fin = plan.circulaireFin ?: return base
        val ideal = if (debut != null) finJoursIdeaux(debut, fin) else null
        return if (ideal != null && !LocalDate.now().isAfter(ideal)) {
            "$base Lo ideal es ir antes del ${diaYFechaCorta(ideal)}: las ofertas se agotan rápido."
        } else {
            "$base Ofertas hasta el ${diaYFechaCorta(fin)}: ve lo antes posible."
        }
    }
    override val notifErreurTitre = "No se creó el plan de la semana"
    override val notifOuvrirPourPhotos = "Abre la app para agregar tus fotos y crear el plan."
}
