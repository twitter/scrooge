{{#throws}}
@throws(classOf[{{typeName}}])
{{/throws}}
def {{name}}({{fieldParams}}): {{#async}}Future[{{/async}}{{typeName}}{{#async}}]{{/async}}
