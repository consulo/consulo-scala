package org.jetbrains.plugins.scala.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.ast.TokenSet;

// upstream: @ApiStatus.Internal
@ExtensionAPI(ComponentScope.APPLICATION)
abstract public class MemberElementTypesExtension {
  public static ExtensionPointName<MemberElementTypesExtension> EP_NAME = ExtensionPointName.create(MemberElementTypesExtension.class);
  
  public TokenSet getMemberElementTypes() {
    return TokenSet.EMPTY;
  }
  
  public static TokenSet getExtraMemberTypes() {
    TokenSet types = TokenSet.EMPTY;
    
    for (MemberElementTypesExtension extension : EP_NAME.getExtensions()) {
      types = TokenSet.orSet(types, extension.getMemberElementTypes());
    }
    
    return types;
  }
}
