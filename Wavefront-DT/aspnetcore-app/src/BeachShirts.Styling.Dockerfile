﻿FROM microsoft/dotnet:2.1-sdk AS build-env
WORKDIR /app

# copy everything and build the project
COPY . ./
RUN dotnet restore BeachShirts.Common/*.csproj
RUN dotnet publish BeachShirts.Styling/*.csproj -c Release -o out

# build runtime image
FROM microsoft/dotnet:2.1-runtime AS runtime
WORKDIR /app
COPY --from=build-env /app/BeachShirts.Styling/out ./
ENTRYPOINT ["dotnet", "BeachShirts.Styling.dll"]