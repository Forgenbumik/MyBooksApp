package com.example.mybooksapp

import com.kursx.parser.fb2.Body
import com.kursx.parser.fb2.FictionBook
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

class Fb2Viewer {
    fun View() {
        try {
            var fb2 = FictionBook(File("book.fb2"))
            var body = fb2.getBody()

        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        catch (e: SAXException) {
            e.printStackTrace()
        }

    }

}