{{docstring}}
{{#throws}}
@throws(classOf[{{typeName}}])
{{/throws}}
def {{funcName}}({{fieldParams}}): {{#async}}Future[{{/async}}{{typeName}}{{#async}}]{{/async}}