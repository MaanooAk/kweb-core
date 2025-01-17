package io.kweb.state.render

import io.github.bonigarcia.seljup.*
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kweb.Kweb
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.creation.tags.InputType.text
import io.kweb.dom.element.new
import io.kweb.state.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
/*
fun main() {
    RenderTestApp()
    Thread.sleep(1000000)
}

*/
@ExtendWith(SeleniumExtension::class)
class RenderTest {
    companion object {
        private lateinit var renderTestApp: RenderTestApp

        @JvmStatic
        @BeforeAll
        fun setupServer() {
            renderTestApp = RenderTestApp()
        }

        @JvmStatic
        @AfterAll
        fun teardownServer() {
            renderTestApp.server.close()
        }

        //selenium-jupiter will automatically fall back if the first browser it tries doesn't work
        //https://bonigarcia.github.io/selenium-jupiter/#generic-driver
        @Options
        var chromeOptions = ChromeOptions().apply {
            setHeadless(true)
        }

        @Options
        var firefoxOptions = FirefoxOptions().apply {
            setHeadless(true)
        }
    }

    @Test
    fun initialRender(driver : WebDriver) {
        driver.get("http://localhost:7659/")
        val h1 = driver.findElement<WebElement>(By.tagName("H1"))
        h1.shouldNotBeNull()
    }
}

class RenderTestApp {
    val server: Kweb = Kweb(port = 7659) {
        doc.body.new {
            val outerKvar = KVar(false)
            val innerKvar = KVar(false)
            render(outerKvar) {
                if (it) {
                    h1().text("Outer: True")
                    render(innerKvar) {
                        if (it) {
                            h2().text("Outer: True, Inner: True")
                        } else {
                            h2().text("Outer: True, Inner: False")
                        }
                    }
                } else {
                    h1().text("Outer: false")
                    render(innerKvar) {
                        if (it) {
                            h2().text("Outer: False, Inner: True")
                        } else {
                            h2().text("Outer: False, Inner: False")
                        }
                    }
                }
            }

            input(type = text).value = outerKvar.map(stringBool)
            input(type = text).value = innerKvar.map(stringBool)
        }
    }

}

private val stringBool = object : ReversableFunction<Boolean, String>(label = "bool -> string") {
    override fun invoke(from: Boolean) = if (from) "true" else "false"
    override fun reverse(original: Boolean, change: String) = change == "true"
}