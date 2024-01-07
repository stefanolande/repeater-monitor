package services

import munit.FunSuite

class APRSHistoryServiceTest extends FunSuite {

  test("parse html webpage") {
    val page = """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                 |	"http://www.w3.org/TR/REC-html40/loose.dtd"><HTML>
                 |<HEAD>
                 |   <meta http-equiv="expires" content="-1">
                 |   <meta http-equiv="pragma" content="no-cache">
                 |<TITLE>Raw Data: IR0UBN</TITLE>
                 |</HEAD>
                 |<BODY alink="#008000" bgcolor="#F5F5DC" link="#0000FF" vlink="#000080">
                 |<tt>
                 |20231221011512,IR0UBN&gt;APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#211,11.77,0.33,5.12,12.33,15.00,11110000<br>
                 |20231221012013,IR0UBN&gt;APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#212,11.76,0.34,5.12,12.32,15.00,11110000<br>
                 |20231221012514,IR0UBN&gt;APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#213,11.75,0.37,5.12,12.32,15.00,11110000<br>
                 |</tt>
                 |</HTML>""".stripMargin

    val result = Seq(
      "20231221011512,IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#211,11.77,0.33,5.12,12.33,15.00,11110000",
      "20231221012013,IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#212,11.76,0.34,5.12,12.32,15.00,11110000",
      "20231221012514,IR0UBN>APDW16,WIDE1-1,qAR,IW0URG-12,IW0URG-JS:T#213,11.75,0.37,5.12,12.32,15.00,11110000"
    )
    assertEquals(APRSHistoryService.parseHtml(page), result)
  }
}
