package cm.inf563.github.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;

/**
 * Cycle de vie du navigateur pour les scénarios :
 * un seul Chromium pour toute la suite (lent à démarrer), mais un contexte
 * neuf par scénario (cookies et session vides) : les scénarios sont isolés.
 * <p>
 * Options : {@code -Dheadless=false} pour voir le navigateur,
 * {@code -DslowMo=500} pour ralentir chaque action (démonstration),
 * {@code -Dcanal=chrome} pour utiliser le Google Chrome déjà installé plutôt que
 * le Chromium téléchargé par Playwright (en CI : aucun téléchargement).
 */
public class Navigateur {

    private static Playwright playwright;
    private static Browser chromium;

    private BrowserContext contexte;
    private Page page;

    @BeforeAll
    public static void demarrer() {
        playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(System.getProperty("headless", "true")))
                .setSlowMo(Double.parseDouble(System.getProperty("slowMo", "0")));
        String canal = System.getProperty("canal", "");
        if (!canal.isBlank()) {
            options.setChannel(canal);
        }
        chromium = playwright.chromium().launch(options);
    }

    @AfterAll
    public static void arreter() {
        chromium.close();
        playwright.close();
    }

    @Before(order = 0)
    public void ouvrirContexte() {
        // Interface GitHub en anglais : les textes vérifiés ne dépendent pas de la langue du poste
        contexte = chromium.newContext(new Browser.NewContextOptions()
                .setLocale("en-US")
                .setViewportSize(1280, 900));
        contexte.setDefaultTimeout(20_000);
        page = contexte.newPage();
    }

    @After
    public void fermerContexte(Scenario scenario) {
        if (scenario.isFailed()) {
            // Capture jointe au rapport HTML Cucumber pour diagnostiquer l'échec
            scenario.attach(page.screenshot(new Page.ScreenshotOptions().setFullPage(true)),
                    "image/png", "capture au moment de l'echec");
        }
        contexte.close();
    }

    public Page page() {
        return page;
    }
}
