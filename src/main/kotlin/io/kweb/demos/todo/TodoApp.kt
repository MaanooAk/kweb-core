package io.kweb.demos.todo

import io.kweb.*
import io.kweb.dom.BodyElement
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.creation.tags.InputType.text
import io.kweb.dom.element.events.on
import io.kweb.dom.element.new
import io.kweb.dom.title
import io.kweb.plugins.fomanticUI.*
import io.kweb.routing.route
import io.kweb.state.*
import io.kweb.state.render.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import java.nio.file.Paths
import java.time.Instant
import java.util.*

fun main(){
    TodoApp()
}

class TodoApp {

    private val logger = KotlinLogging.logger {}

    val state = ToDoState(Paths.get("data"))
    val plugins = listOf(fomanticUIPlugin)
    val server:Kweb

    init {

        /** Create a Kweb instance, and configure it to use the Fomantic
         * UI framework. Build a simple to-do list app listening on
         * http://localhost:7659/
         * */
        server = Kweb(port = 7659, debug = true, plugins = plugins, buildPage = {
            doc.body.new {
                /** Kweb allows you to modularize your code however suits your needs
                best.  Here I use an extension function defined elsewhere to
                draw some common outer page DOM elements */
                pageBorderAndTitle("To do List") {
                    div(fomantic.content).new {

                        route {

                            path("/") {
                                val newListId = createNewList()
                                /**
                                 * By updating the URL path this will cause the page to switch to the newly created list
                                 * automatically, and without a page refresh.
                                 */
                                url.path.value = "/lists/$newListId"
                            }

                            path("/lists/{id}") { params ->
                                doc.head.new {
                                    title().text("To Do List #" + params.getValue("id"))
                                }
                                render(params.getValue("id")) { listId ->
                                    logger.info("Rendering list id $listId")

                                    try {
                                        /** Here I use the same render mechanism to tie DOM
                                        state to persistent state stored in Shoebox, a simple but powerful
                                        key-value store with observer pattern support. */
                                        renderList(toVar(state.lists, listId))
                                    } catch (e: NoSuchElementException) {
                                        throw NotFoundException("Can't find list with id $listId")
                                    }
                                }
                            }

                            /*
                                 * It's not necessary, but we can also define a custom 404 handler:
                                 */
                            notFound {
                                div(fomantic.ui.negative.message).new {
                                    div(fomantic.header).text("Not Found :(")
                                    p().text(url.path.map { "Unable to find path $it" })
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun ElementCreator<BodyElement>.pageBorderAndTitle(title: String, content: ElementCreator<DivElement>.() -> Unit) {
        div(fomantic.ui.main.container).new {
            div(fomantic.column).new {
                div(fomantic.ui.vertical.segment).new {
                    div(fomantic.ui.message).new {
                        p().innerHTML(
                                """
                            A simple demo of <a href="https://docs.kweb.io/">Kweb</a>, add and delete items from a
                            to do list.
                            <p/>
                            Try visiting this URL in another browser window and make some changes.
                            <p/>
                            You may find the source code for this app
                            <a href="https://github.com/kwebio/core/tree/master/src/main/kotlin/io/kweb/demos/todo">here</a>.
                            """
                                        .trimIndent()
                        )
                    }
                }

                div(fomantic.ui.vertical.segment).new {
                    h1(fomantic.ui.dividing.header).text(title)
                    content(this)
                }
            }
        }
    }

    private fun createNewList(): String {
        val newListId = generateNewUid()
        state.lists[newListId] = ToDoState.List(newListId, "")
        return newListId
    }

    private fun ElementCreator<*>.renderList(list: KVar<ToDoState.List>) {
        h3().text(list.map(ToDoState.List::title))
        div(fomantic.ui.middle.aligned.divided.list).new {
            renderEach(state.itemsByList(list.value.uid)) { item ->
                div(fomantic.item).new {
                    div(fomantic.right.floated.content).new {
                        renderRemoveButton(item)
                    }
                    div(fomantic.content).text(item.map(ToDoState.Item::text))
                }
            }
        }
        div(fomantic.ui.action.input).new {
            val input = input(text, placeholder = "Add Item")
            input.on.keypress { ke ->
                if (ke.code == "Enter") {
                    handleAddItem(input, list)
                }
            }
            button(fomantic.ui.button).text("Add").apply {
                on.click {
                    handleAddItem(input, list)
                }
            }
        }
    }

    private fun handleAddItem(input: InputElement, list: KVar<ToDoState.List>) {
        GlobalScope.launch {
            val newItemText = input.getValue().await()
            input.setValue("")
            val newItem = ToDoState.Item(generateNewUid(), Instant.now(), list.value.uid, newItemText)
            state.items[newItem.uid] = newItem
        }
    }

    private fun ElementCreator<DivElement>.renderRemoveButton(item: KVar<ToDoState.Item>) {
        val button = button(fomantic.mini.ui.icon.button)
        button.new {
            i(fomantic.trash.icon)
        }
        button.on.click {
            state.items.remove(item.value.uid)
        }
    }

    private fun generateNewUid() = random.nextInt(100_000_000).toString(16)
}